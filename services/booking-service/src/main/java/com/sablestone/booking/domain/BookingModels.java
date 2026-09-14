package com.sablestone.booking.domain;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

/** Shared immutable models for the first in-memory availability implementation. */
public final class BookingModels {

    private BookingModels() {
        // Utility holder; instances are not required.
    }

    public record ServiceDefinition(
            String id,
            String name,
            int durationMinutes,
            int priceZar,
            boolean requiresApproval
    ) {}

    public record TimePeriod(LocalTime start, LocalTime end, String reason) {}

    public record ExistingBooking(LocalDate date, LocalTime start, LocalTime end, String status) {}

    public record AvailabilitySlot(
            LocalDateTime start,
            LocalDateTime end,
            String label
    ) {}

    public record AvailabilityResponse(
            String serviceId,
            LocalDate date,
            int durationMinutes,
            int bufferMinutes,
            List<AvailabilitySlot> slots
    ) {}
}

