package com.sablestone.booking.repository;

import com.sablestone.booking.domain.BookingModels;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Reads active catalogue services from PostgreSQL.
 *
 * <p>The repository owns SQL and row mapping. It does not calculate slots or
 * decide whether a service requires approval; those responsibilities remain in
 * the service layer.</p>
 */

@Repository
public class ServiceRepository {

    private final JdbcTemplate jdbcTemplate;

    public ServiceRepository(JdbcTemplate jdbcTemplate) {
        // Spring supplies JdbcTemplate using the configured DataSource.
        this.jdbcTemplate = jdbcTemplate;
    }

    /** Finds one active service by its stable public identifier. */
    public Optional<BookingModels.ServiceDefinition> findActiveById(String serviceId) {
        String sql = """
                SELECT id, name, duration_minutes, price_zar, requires_approval
                FROM services
                WHERE id = ? AND active = TRUE
                """;

        return jdbcTemplate.query(sql, (resultSet, rowNumber) -> new BookingModels.ServiceDefinition(
                        resultSet.getString("id"),
                        resultSet.getString("name"),
                        resultSet.getInt("duration_minutes"),
                        resultSet.getInt("price_zar"),
                        resultSet.getBoolean("requires_approval")
                ), serviceId)
                .stream()
                .findFirst();
    }
}

