package com.sablestone.booking.service;

import com.sablestone.booking.domain.BookingModels;
import com.sablestone.booking.domain.BookingRequests;
import com.sablestone.booking.repository.BookingRepository;
import com.sablestone.booking.repository.ServiceRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * Coordinates booking validation and persistence.
 *
 * <p>This is deliberately separate from the availability calculator. A slot
 * can become unavailable after the frontend has displayed it, so creation must
 * perform a final conflict check inside a database transaction.</p>
 */
@Service
public class BookingService {

    private final BookingRepository bookingRepository;
    private final ServiceRepository serviceRepository;

    public BookingService(BookingRepository bookingRepository, ServiceRepository serviceRepository) {
        this.bookingRepository = bookingRepository;
        this.serviceRepository = serviceRepository;
    }

    /**
     * Validates and creates a pending booking.
     *
     * @throws IllegalArgumentException when the service does not exist
     * @throws SlotUnavailableException when another active booking overlaps
     */
    @Transactional
    public BookingRequests.BookingResponse create(BookingRequests.CreateBookingRequest request) {
        BookingModels.ServiceDefinition service = serviceRepository.findActiveById(request.serviceId())
                .orElseThrow(() -> new IllegalArgumentException("Unknown service: " + request.serviceId()));

        LocalDateTime start = LocalDateTime.of(request.preferredDate(), request.preferredTime());
        LocalDateTime end = start.plusMinutes(service.durationMinutes());
        LocalDateTime occupiedEnd = end.plusMinutes(AvailabilityService.BUFFER_MINUTES);

        // The same slot rules used for display must be rechecked before insertion.
        if (bookingRepository.hasActiveOverlap(start, occupiedEnd)) {
            throw new SlotUnavailableException(start, end);
        }

        try {
            String reference = bookingRepository.insert(request, start, end, service.requiresApproval());
            String approval = service.requiresApproval() ? "PENDING_REVIEW" : "APPROVED";
            return new BookingRequests.BookingResponse(reference, service.id(), request.preferredDate(),
                    request.preferredTime(), end.toLocalTime(), "PENDING", approval, "NOT_STARTED");
        } catch (DataIntegrityViolationException exception) {
            // A database constraint can still reject a race; expose a safe conflict response.
            throw new SlotUnavailableException(start, end);
        }
    }

    /** Domain exception translated by the controller into HTTP 409 Conflict. */
    public static class SlotUnavailableException extends RuntimeException {
        private final LocalDateTime start;
        private final LocalDateTime end;

        public SlotUnavailableException(LocalDateTime start, LocalDateTime end) {
            super("The selected appointment time is no longer available.");
            this.start = start;
            this.end = end;
        }

        public LocalDateTime start() { return start; }
        public LocalDateTime end() { return end; }
    }
}
