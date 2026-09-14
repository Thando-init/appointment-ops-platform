package com.sablestone.booking.domain;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.time.LocalTime;

/**
 * HTTP payloads for creating a booking.
 *
 * <p>The client sends a service and requested start time. The server derives
 * the appointment end time from the trusted service duration instead of
 * trusting a client-supplied duration or end time.</p>
 */
public final class BookingRequests {

    private BookingRequests() {
        // Utility holder; request records are used directly.
    }

    /** Data submitted by the website, WhatsApp workflow, or n8n. */
    public record CreateBookingRequest(
            @NotBlank String clientName,
            @NotBlank String phone,
            @Email String email,
            @NotBlank String serviceId,
            @NotNull LocalDate preferredDate,
            @NotNull LocalTime preferredTime,
            String notes,
            String source
    ) {}

    /** Stable response returned after the booking row has been created. */
    public record BookingResponse(
            String bookingReference,
            String serviceId,
            LocalDate date,
            LocalTime start,
            LocalTime end,
            String bookingStatus,
            String approvalStatus,
            String paymentStatus
    ) {}
}
