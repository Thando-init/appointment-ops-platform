package com.sablestone.booking.api;

import com.sablestone.booking.domain.BookingModels;
import com.sablestone.booking.domain.BookingRequests;
import com.sablestone.booking.service.BookingService;
import com.sablestone.booking.service.AvailabilityService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;

/**
 * HTTP boundary consumed by the static booking form and, later, n8n.
 *
 * <p>This class should remain thin: it translates HTTP input into Java types,
 * delegates business rules to {@link AvailabilityService}, and translates
 * known input failures into HTTP responses. Availability rules do not belong
 * in the controller because they must also be reusable by booking creation and
 * conflict checks.</p>
 *
 * <p>The current CORS list is intentionally limited to local frontend origins.
 * Production origins should be configured explicitly rather than allowing
 * every website to call the API.</p>
 */

@RestController
@RequestMapping("/api/v1")
@CrossOrigin(origins = {"http://localhost:8000", "http://127.0.0.1:8000"})
public class AvailabilityController {

    private final AvailabilityService availabilityService;
    private final BookingService bookingService;

    public AvailabilityController(AvailabilityService availabilityService, BookingService bookingService) {
        // Constructor injection makes this dependency explicit and testable.
        this.availabilityService = availabilityService;
        this.bookingService = bookingService;
    }

    /**
     * Lightweight process check used by local scripts, Docker, and monitoring.
     */
    @GetMapping("/health")
    public HealthResponse health() {
        return new HealthResponse("ok", "booking-api");
    }

    /**
     * Returns start times that can fit the requested service on the requested date.
     *
     * @param serviceId stable catalogue identifier, for example {@code gel-overlay-art}
     * @param date requested local calendar date in {@code YYYY-MM-DD} format
     * @return service metadata and zero or more available slots
     * @throws ResponseStatusException with HTTP 400 when the service identifier is unknown
     */
    @GetMapping("/availability/slots")
    public BookingModels.AvailabilityResponse slots(
            @RequestParam String serviceId,
            @RequestParam LocalDate date
    ) {
        try {
            return availabilityService.getSlots(serviceId, date);
        } catch (IllegalArgumentException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, exception.getMessage(), exception);
        }
    }

    /**
     * Creates a pending booking after repeating the final slot conflict check.
     *
     * @param request validated client and appointment data
     * @return persisted booking reference and independent workflow statuses
     */
    @PostMapping("/bookings")
    public BookingRequests.BookingResponse createBooking(
            @Valid @RequestBody BookingRequests.CreateBookingRequest request
    ) {
        try {
            return bookingService.create(request);
        } catch (BookingService.SlotUnavailableException exception) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, exception.getMessage(), exception);
        } catch (BookingService.ScheduleUnavailableException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, exception.getMessage(), exception);
        } catch (IllegalArgumentException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, exception.getMessage(), exception);
        }
    }

    /** Returns current persisted state for the n8n authoritative-read step. */
    @GetMapping("/bookings/{bookingReference}")
    public BookingRequests.BookingDetails getBooking(
            @PathVariable String bookingReference
    ) {
        return bookingService.findByReference(bookingReference)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Booking not found: " + bookingReference));
    }

    /** Demo adapter endpoint for the owner approval step in n8n. */
    @PostMapping("/bookings/{bookingReference}/approval")
    public BookingRequests.BookingDetails updateApproval(
            @PathVariable String bookingReference,
            @RequestBody BookingRequests.ApprovalRequest request
    ) {
        try {
            return bookingService.updateApproval(bookingReference, request.approvalStatus());
        } catch (BookingService.BookingNotFoundException | IllegalArgumentException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, exception.getMessage(), exception);
        }
    }

    /** Demo adapter endpoint for payment-provider confirmation. */
    @PostMapping("/bookings/{bookingReference}/payment-confirmation")
    public BookingRequests.BookingDetails confirmPayment(
            @PathVariable String bookingReference,
            @RequestBody BookingRequests.PaymentConfirmationRequest request
    ) {
        try {
            return bookingService.updatePayment(bookingReference, request.paymentStatus());
        } catch (BookingService.BookingNotFoundException | IllegalArgumentException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, exception.getMessage(), exception);
        }
    }

    /** Returns a deterministic fake payment link for local demos and screenshots. */
    @PostMapping("/bookings/{bookingReference}/payment-link")
    public BookingRequests.PaymentLinkResponse createPaymentLink(@PathVariable String bookingReference) {
        bookingService.findByReference(bookingReference)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Booking not found: " + bookingReference));
        return new BookingRequests.PaymentLinkResponse(
                bookingReference,
                "demo-pay-" + bookingReference,
                "http://localhost:8080/demo-payment/" + bookingReference,
                "demo");
    }

    /** Demo adapter endpoint representing successful calendar event creation. */
    @PostMapping("/bookings/{bookingReference}/calendar-confirmation")
    public BookingRequests.BookingDetails confirmCalendar(@PathVariable String bookingReference) {
        try {
            return bookingService.confirmCalendar(bookingReference);
        } catch (BookingService.BookingNotFoundException | IllegalArgumentException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, exception.getMessage(), exception);
        }
    }

    /** Stable response shape for health checks. */
    public record HealthResponse(String status, String service) {}
}
