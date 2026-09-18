package com.sablestone.booking.messaging;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;

/**
 * The small message sent after a booking has been saved successfully.
 *
 * <p>This is deliberately separate from the HTTP request. The browser sends
 * a request to create a booking, while this message describes a booking event
 * that other systems can consume. Keeping the event shape stable makes it
 * easier to connect n8n, another Java service, or a future audit process.</p>
 */
public record BookingCreatedMessage(
        String eventId,
        String eventType,
        Instant occurredAt,
        String bookingReference,
        String bookingStatus,
        String approvalStatus,
        String paymentStatus,
        Client client,
        Appointment appointment
) {

    /** Client details needed by the first notification workflow. */
    public record Client(String name, String phone, String email) {}

    /** Appointment details used by n8n and future calendar integration. */
    public record Appointment(
            String serviceId,
            LocalDate date,
            LocalTime start,
            LocalTime end
    ) {}
}
