package com.sablestone.booking.service;

import java.com.sablestone.booking.domain.BookingModels;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AvailabilityServiceTest {

    private final AvailabilityService availabilityService = new AvailabilityService();

    @Test
    void returnsSlotsForAnOpenDate() {
        BookingModels.AvailabilityResponse response = availabilityService.getSlots(
                "gel-overlay-art",
                LocalDate.of(2026, 9, 17)
        );

        assertEquals(120, response.durationMinutes());
        assertEquals(15, response.bufferMinutes());
        assertEquals(14, response.slots().size());
        assertEquals("08:00", response.slots().getFirst().label());
    }

    @Test
    void excludesBlockedPeriodsAndKeepsFullAppointmentWithinHours() {
        BookingModels.AvailabilityResponse response = availabilityService.getSlots(
                "gel-overlay-art",
                LocalDate.of(2026, 10, 3)
        );

        assertEquals(4, response.slots().size());
        assertEquals("13:00", response.slots().getFirst().label());
        assertEquals("14:30", response.slots().getLast().label());
    }

    @Test
    void returnsNoSlotsOnClosedDays() {
        BookingModels.AvailabilityResponse response = availabilityService.getSlots(
                "gel-overlay",
                LocalDate.of(2026, 9, 14)
        );

        assertTrue(response.slots().isEmpty());
    }
}
