package com.sablestone.booking.repository;

import com.sablestone.booking.domain.BookingRequests;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.UUID;

/** Persists booking requests and performs the final overlap check. */
@Repository
public class BookingRepository {

    private final JdbcTemplate jdbcTemplate;

    public BookingRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * Checks active bookings using half-open interval logic.
     * A booking ending exactly when another starts is not considered an overlap.
     */
    public boolean hasActiveOverlap(LocalDateTime start, LocalDateTime end) {
        String sql = """
                SELECT EXISTS (
                    SELECT 1 FROM bookings
                    WHERE booking_status IN ('PENDING', 'CONFIRMED')
                      AND starts_at < ?
                      AND ends_at > ?
                )
                """;
        return Boolean.TRUE.equals(jdbcTemplate.queryForObject(sql, Boolean.class, end, start));
    }

    /** Inserts a new pending booking after the service layer has validated its slot. */
    public String insert(
            BookingRequests.CreateBookingRequest request,
            LocalDateTime start,
            LocalDateTime end,
            boolean requiresApproval
    ) {
        String reference = "SALON-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        String sql = """
                INSERT INTO bookings (
                    booking_reference, service_id, client_name, phone, email,
                    starts_at, ends_at, booking_status, approval_status,
                    payment_status, source, notes
                ) VALUES (?, ?, ?, ?, ?, ?, ?, 'PENDING', ?, 'NOT_STARTED', ?, ?)
                """;
        String approvalStatus = requiresApproval ? "PENDING_REVIEW" : "APPROVED";
        jdbcTemplate.update(sql, reference, request.serviceId(), request.clientName(), request.phone(),
                request.email(), start, end, approvalStatus,
                request.source() == null || request.source().isBlank() ? "website" : request.source(),
                request.notes());
        return reference;
    }
}
