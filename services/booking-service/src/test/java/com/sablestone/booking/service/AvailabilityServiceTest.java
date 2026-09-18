package com.sablestone.booking.service;

import com.sablestone.booking.domain.BookingModels;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;


/** Documents the expected scheduling behaviour before database repositories are introduced. */
class AvailabilityServiceTest {

    private final AvailabilityService availabilityService = new AvailabilityService();

    @Test
    /** An ordinary open date should return service-duration-aware slots. */
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
    /** Blocked periods remove candidates whose service or cleanup buffer overlaps them. */
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
    /** Closed weekdays are successful lookups with no bookable start times. */
    void returnsNoSlotsOnClosedDays() {
        BookingModels.AvailabilityResponse response = availabilityService.getSlots(
                "gel-overlay",
                LocalDate.of(2026, 9, 14)
        );

        assertTrue(response.slots().isEmpty());
    }
}

