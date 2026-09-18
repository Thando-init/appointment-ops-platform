package com.sablestone.booking.repository;

import com.sablestone.booking.domain.BookingModels;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

/**
 * Reads schedule exclusions from PostgreSQL.
 *
 * <p>Only records that overlap the requested date are loaded. Keeping this
 * filtering in SQL avoids pulling the entire booking history into memory.</p>
 */

@Repository
public class ScheduleRepository {

    private final JdbcTemplate jdbcTemplate;

    public ScheduleRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /** Loads the normal opening window for the requested ISO weekday. */
    public Optional<BookingModels.BusinessHours> findBusinessHours(DayOfWeek day) {
        String sql = """
                SELECT opens_at, closes_at
                FROM business_hours
                WHERE day_of_week = ?
                """;

        return jdbcTemplate.query(sql, (resultSet, rowNumber) -> new BookingModels.BusinessHours(
                        resultSet.getTime("opens_at").toLocalTime(),
                        resultSet.getTime("closes_at").toLocalTime()
                ), day.getValue())
                .stream()
                .findFirst();
    }

    /** Returns blocked periods that intersect any time on the requested date. */
    public List<BookingModels.TimePeriod> findBlockedPeriods(LocalDate date) {
        String sql = """
                SELECT starts_at::time AS starts_at, ends_at::time AS ends_at, reason
                FROM blocked_periods
                WHERE starts_at < (?::date + INTERVAL '1 day')
                  AND ends_at > ?::date
                ORDER BY starts_at
                """;

        return jdbcTemplate.query(sql, (resultSet, rowNumber) -> new BookingModels.TimePeriod(
                resultSet.getTime("starts_at").toLocalTime(),
                resultSet.getTime("ends_at").toLocalTime(),
                resultSet.getString("reason")
        ), date, date);
    }

    /**
     * Returns active bookings whose occupied intervals overlap the requested date.
     * The 15-minute buffer is added by the availability service, not duplicated here.
     */
    public List<BookingModels.ExistingBooking> findActiveBookings(LocalDate date) {
        String sql = """
                SELECT starts_at::time AS starts_at, ends_at::time AS ends_at,
                       booking_status
                FROM bookings
                WHERE starts_at < (?::date + INTERVAL '1 day')
                  AND ends_at > ?::date
                  AND booking_status IN ('PENDING', 'CONFIRMED')
                ORDER BY starts_at
                """;

        return jdbcTemplate.query(sql, (resultSet, rowNumber) -> new BookingModels.ExistingBooking(
                date,
                resultSet.getTime("starts_at").toLocalTime(),
                resultSet.getTime("ends_at").toLocalTime(),
                resultSet.getString("booking_status")
        ), date, date);
    }
}

