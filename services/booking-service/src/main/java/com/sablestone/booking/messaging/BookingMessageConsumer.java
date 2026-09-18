package com.sablestone.booking.messaging;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.stereotype.Component;

/**
 * Receives booking-created messages from ActiveMQ.
 *
 * <p>The first consumer only parses and logs the message. This is intentional:
 * it lets me prove that JMS delivery works before adding an HTTP request to
 * n8n. Later this class can call an n8n webhook or delegate to a separate
 * notification client.</p>
 */
@Component
public class BookingMessageConsumer {

    private static final Logger log = LoggerFactory.getLogger(BookingMessageConsumer.class);

    private final ObjectMapper objectMapper;

    public BookingMessageConsumer(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    /** Receives one JSON message from the booking.created queue. */
    @JmsListener(destination = "${app.messaging.booking-created-queue}")
    public void receive(String json) {
        try {
            com.sablestone.booking.messaging.BookingCreatedMessage message = objectMapper.readValue(json, com.sablestone.booking.messaging.BookingCreatedMessage.class);
            log.info("Received {} event for booking {}", message.eventType(), message.bookingReference());
        } catch (JsonProcessingException exception) {
            // Throwing causes the JMS listener container to treat the message
            // as unsuccessful instead of pretending that malformed data worked.
            throw new IllegalArgumentException("Received invalid booking event JSON", exception);
        }
    }
}
