package com.sablestone.booking.messaging;

import com.sablestone.booking.domain.BookingRequests;

import java.time.Instant;

/**
 * Internal application event raised by the booking service.
 *
 * <p>The event is published inside the service transaction, but its listener
 * waits until the transaction commits before sending anything to ActiveMQ.
 * This prevents a message from being published for a booking that later rolls
 * back because of a database failure.</p>
 */
public record BookingCreatedEvent(
        BookingRequests.CreateBookingRequest request,
        BookingRequests.BookingResponse response,
        Instant occurredAt
) {
}
