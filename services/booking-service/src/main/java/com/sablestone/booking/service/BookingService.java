package com.sablestone.booking.service;

import com.sablestone.booking.domain.BookingModels;
import com.sablestone.booking.domain.BookingRequests;
import com.sablestone.booking.messaging.BookingCreatedEvent;
import com.sablestone.booking.repository.BookingRepository;
import com.sablestone.booking.repository.ScheduleRepository;
import com.sablestone.booking.repository.ServiceRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
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
    private final ScheduleRepository scheduleRepository;
    private final ApplicationEventPublisher eventPublisher;

    public BookingService(BookingRepository bookingRepository, ServiceRepository serviceRepository,
                          ScheduleRepository scheduleRepository,
                          ApplicationEventPublisher eventPublisher) {
        this.bookingRepository = bookingRepository;
        this.serviceRepository = serviceRepository;
        this.scheduleRepository = scheduleRepository;
        this.eventPublisher = eventPublisher;
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
        LocalDateTime occupiedEnd = end.plusMinutes(com.sablestone.booking.service.AvailabilityService.BUFFER_MINUTES);

        validateBusinessSchedule(request, start, occupiedEnd);

        // The same slot rules used for display must be rechecked before insertion.
        if (bookingRepository.hasActiveOverlap(start, occupiedEnd)) {
            throw new SlotUnavailableException(start, end);
        }

        try {
            String reference = bookingRepository.insert(request, start, end, service.requiresApproval());
            String approval = service.requiresApproval() ? "PENDING_REVIEW" : "APPROVED";
            BookingRequests.BookingResponse response = new BookingRequests.BookingResponse(
                    reference, service.id(), request.preferredDate(),
                    request.preferredTime(), end.toLocalTime(), "PENDING", approval, "NOT_STARTED");
            // The listener sends this to ActiveMQ only after this transaction commits.
            eventPublisher.publishEvent(new BookingCreatedEvent(request, response, Instant.now()));
            return response;
        } catch (DataIntegrityViolationException exception) {
            // A database constraint can still reject a race; expose a safe conflict response.
            throw new SlotUnavailableException(start, end);
        }
    }

    /** Ensures a hand-crafted request obeys the same schedule rules as the UI. */
    private void validateBusinessSchedule(BookingRequests.CreateBookingRequest request,
                                          LocalDateTime start, LocalDateTime occupiedEnd) {
        BookingModels.BusinessHours hours = scheduleRepository.findBusinessHours(
                        request.preferredDate().getDayOfWeek())
                .orElseThrow(() -> new ScheduleUnavailableException("The studio is closed on the selected date."));

        LocalDateTime opening = LocalDateTime.of(request.preferredDate(), hours.opensAt());
        LocalDateTime closing = LocalDateTime.of(request.preferredDate(), hours.closesAt());
        if (start.isBefore(opening) || occupiedEnd.isAfter(closing)) {
            throw new ScheduleUnavailableException("The appointment does not fit within business hours.");
        }

        boolean overlapsBlocked = scheduleRepository.findBlockedPeriods(request.preferredDate()).stream()
                .anyMatch(period -> request.preferredTime().isBefore(period.end())
                        && occupiedEnd.toLocalTime().isAfter(period.start()));
        if (overlapsBlocked) {
            throw new ScheduleUnavailableException("The selected time overlaps a blocked period.");
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

    /** Indicates a valid service request at an invalid studio time. */
    public static class ScheduleUnavailableException extends RuntimeException {
        public ScheduleUnavailableException(String message) {
            super(message);
        }
    }
}
