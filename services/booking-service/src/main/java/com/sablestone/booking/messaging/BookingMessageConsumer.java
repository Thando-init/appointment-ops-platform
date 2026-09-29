package com.sablestone.booking.messaging;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

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
    private final String n8nWebhookUrl;
    private final HttpClient httpClient = HttpClient.newHttpClient();

    public BookingMessageConsumer(ObjectMapper objectMapper) {
        this(objectMapper, "");
    }

    @Autowired
    public BookingMessageConsumer(
            ObjectMapper objectMapper,
            @Value("${app.n8n.booking-created-webhook-url:}") String n8nWebhookUrl
    ) {
        this.objectMapper = objectMapper;
        this.n8nWebhookUrl = n8nWebhookUrl;
    }

    /** Receives one JSON message from the booking.created queue. */
    @JmsListener(destination = "${app.messaging.booking-created-queue}")
    public void receive(String json) {
        try {
            com.sablestone.booking.messaging.BookingCreatedMessage message = objectMapper.readValue(json, com.sablestone.booking.messaging.BookingCreatedMessage.class);
            log.info("Received {} event for booking {}", message.eventType(), message.bookingReference());
            forwardToN8n(json, message.bookingReference());
        } catch (JsonProcessingException exception) {
            // Throwing causes the JMS listener container to treat the message
            // as unsuccessful instead of pretending that malformed data worked.
            throw new IllegalArgumentException("Received invalid booking event JSON", exception);
        }
    }

    private void forwardToN8n(String json, String bookingReference) {
        if (n8nWebhookUrl == null || n8nWebhookUrl.isBlank()) {
            log.debug("n8n forwarding disabled; booking {} remains available on the JMS queue", bookingReference);
            return;
        }
        try {
            HttpRequest request = HttpRequest.newBuilder(URI.create(n8nWebhookUrl))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(json))
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new IllegalStateException("n8n returned HTTP " + response.statusCode());
            }
            log.info("Forwarded booking {} to n8n", bookingReference);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted while forwarding booking to n8n", exception);
        } catch (Exception exception) {
            throw new IllegalStateException("Could not forward booking to n8n", exception);
        }
    }
}
