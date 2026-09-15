package com.sablestone.booking.domain;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

/**
 * Shared immutable models for the availability boundary.
 *
 * <p>These records are grouped temporarily to keep the first iteration small.
 * As the database layer grows, they can be split into one domain type per file
 * without changing the concepts represented here.</p>
 */
public final class BookingModels {

    private BookingModels() {
        // Utility holder; instances are not required.
    }

    /** A catalogue entry used to determine duration, price, and approval flow. */
    public record ServiceDefinition(
            String id,
            String name,
            int durationMinutes,
            int priceZar,
            boolean requiresApproval
    ) {}

    /** A local-time interval that cannot be used for an appointment. */
    public record TimePeriod(LocalTime start, LocalTime end, String reason) {}

    /** Normal opening window for one weekday, loaded from the business_hours table. */
    public record BusinessHours(LocalTime opensAt, LocalTime closesAt) {}

    /** A previously stored appointment that may block a candidate slot. */
    public record ExistingBooking(LocalDate date, LocalTime start, LocalTime end, String status) {}

    /** A client-facing appointment window returned by the availability API. */
    public record AvailabilitySlot(
            LocalDateTime start,
            LocalDateTime end,
            String label
    ) {}

    /** Complete availability result for one service and calendar date. */
    public record AvailabilityResponse(
            String serviceId,
            LocalDate date,
            int durationMinutes,
            int bufferMinutes,
            List<AvailabilitySlot> slots
    ) {}
}
