package com.sablestone.booking.messaging;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sablestone.booking.domain.BookingRequests;
import org.junit.jupiter.api.Test;
import org.springframework.jms.core.JmsTemplate;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

/**
 * Small messaging tests that do not require Docker or a running broker.
 *
 * <p>These tests verify the message shape and the producer's queue choice.
 * A later integration test can prove delivery against a real ActiveMQ
 * container.</p>
 */
class BookingMessagingTest {

    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

    @Test
    void producerSendsBookingCreatedJsonToConfiguredQueue() {
        JmsTemplate jmsTemplate = mock(JmsTemplate.class);
        com.sablestone.booking.messaging.BookingMessageProducer producer = new com.sablestone.booking.messaging.BookingMessageProducer(
                jmsTemplate, objectMapper, "booking.created");

        BookingRequests.CreateBookingRequest request = new BookingRequests.CreateBookingRequest(
                "Amina Patel", "+27821234567", "amina@example.com", "gel-overlay-art",
                LocalDate.of(2026, 10, 3), LocalTime.of(14, 0), "Chrome finish", "website");
        BookingRequests.BookingResponse response = new BookingRequests.BookingResponse(
                "SALON-AB12CD34", "gel-overlay-art", LocalDate.of(2026, 10, 3),
                LocalTime.of(14, 0), LocalTime.of(16, 0), "PENDING", "PENDING_REVIEW", "NOT_STARTED");

        producer.publish(new com.sablestone.booking.messaging.BookingCreatedEvent(request, response, Instant.parse("2026-10-03T10:00:00Z")));

        verify(jmsTemplate).convertAndSend(eq("booking.created"), contains("SALON-AB12CD34"));
    }

    @Test
    void consumerCanReadTheJsonMessageShape() throws Exception {
        com.sablestone.booking.messaging.BookingCreatedMessage message = new com.sablestone.booking.messaging.BookingCreatedMessage(
                "evt-1", "booking.created", Instant.parse("2026-10-03T10:00:00Z"),
                "SALON-AB12CD34", "PENDING", "PENDING_REVIEW", "NOT_STARTED",
                new com.sablestone.booking.messaging.BookingCreatedMessage.Client("Amina Patel", "+27821234567", "amina@example.com"),
                new com.sablestone.booking.messaging.BookingCreatedMessage.Appointment("gel-overlay-art", LocalDate.of(2026, 10, 3),
                        LocalTime.of(14, 0), LocalTime.of(16, 0)));
        com.sablestone.booking.messaging.BookingMessageConsumer consumer = new com.sablestone.booking.messaging.BookingMessageConsumer(objectMapper);

        String json = objectMapper.writeValueAsString(message);

        assertDoesNotThrow(() -> consumer.receive(json));
        assertTrue(json.contains("booking.created"));
        assertTrue(json.contains("SALON-AB12CD34"));
    }
}
