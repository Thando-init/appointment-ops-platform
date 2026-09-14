CREATE TABLE bookings(
    id BIGSERIAL PRIMARY KEY,
    booking_id VARCHAR(40) NOT NULL UNIQUE,

    client_name VARCHAR(120) NOT NULL,
    phone VARCHAR(30) NOT NULL,
    email VARCHAR(255) NOT NULL,

    service_requested VARCHAR(120) NOT NULL,
    preferred_date DATE NOT NULL,
    preferred_time TIME NOT NULL,
    notes TEXT,

    source VARCHAR(30) NOT NULL DEFAULT 'website',

    /**
     Remove confusion  when approval, payment, and booking progress happen independently.
     */
    booking_status VARCHAR(40) NOT NULL DEFAULT 'new_request',
    approval_status VARCHAR(40) NOT NULL DEFAULT 'not_started',
    payment_status VARCHAR(40) NOT NULL DEFAULT 'not_started',

    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP

);

CREATE INDEX idx_bookings_booking_id
    ON bookings (booking_id);

CREATE INDEX idx_bookings_phone
    ON bookings (phone);

CREATE INDEX idx_bookings_status
    ON bookings (booking_status);