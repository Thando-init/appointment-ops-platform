package com.sablestone.booking.messaging;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.UUID;

/**
 * Publishes durable booking events to the ActiveMQ queue.
 *
 * <p>The listener uses {@link TransactionPhase#AFTER_COMMIT}. That means the
 * database transaction finishes successfully before this class sends a JMS
 * message. n8n will eventually receive the same JSON shape through a consumer
 * that we can add after the Java queue flow is understood.</p>
 */
@Component
public class BookingMessageProducer {

    private final JmsTemplate jmsTemplate;
    private final ObjectMapper objectMapper;
    private final String bookingCreatedQueue;

    public BookingMessageProducer(
            JmsTemplate jmsTemplate,
            ObjectMapper objectMapper,
            @Value("${app.messaging.booking-created-queue}") String bookingCreatedQueue
    ) {
        this.jmsTemplate = jmsTemplate;
        this.objectMapper = objectMapper;
        this.bookingCreatedQueue = bookingCreatedQueue;
    }

    /** Sends one booking.created JSON message after the booking commits. */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void publish(com.sablestone.booking.messaging.BookingCreatedEvent event) {
        BookingRequestsAdapter adapter = new BookingRequestsAdapter(event);
        com.sablestone.booking.messaging.BookingCreatedMessage message = adapter.toMessage();

        try {
            String json = objectMapper.writeValueAsString(message);
            jmsTemplate.convertAndSend(bookingCreatedQueue, json);
        } catch (JsonProcessingException exception) {
            // A serialization failure is a programming/configuration problem.
            // Throwing it makes the problem visible instead of silently losing it.
            throw new IllegalStateException("Could not serialize booking-created event", exception);
        }
    }

    /** Small mapping helper kept private to make the event conversion readable. */
    private static final class BookingRequestsAdapter {
        private final com.sablestone.booking.messaging.BookingCreatedEvent event;

        private BookingRequestsAdapter(com.sablestone.booking.messaging.BookingCreatedEvent event) {
            this.event = event;
        }

        private com.sablestone.booking.messaging.BookingCreatedMessage toMessage() {
            var request = event.request();
            var response = event.response();
            return new com.sablestone.booking.messaging.BookingCreatedMessage(
                    UUID.randomUUID().toString(),
                    "booking.created",
                    event.occurredAt(),
                    response.bookingReference(),
                    response.bookingStatus(),
                    response.approvalStatus(),
                    response.paymentStatus(),
                    new com.sablestone.booking.messaging.BookingCreatedMessage.Client(
                            request.clientName(), request.phone(), request.email()),
                    new com.sablestone.booking.messaging.BookingCreatedMessage.Appointment(
                            response.serviceId(), response.date(), response.start(), response.end())
            );
        }
    }
}
