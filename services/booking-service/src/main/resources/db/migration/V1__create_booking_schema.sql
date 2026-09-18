-- V1 creates the durable scheduling primitives used by availability checks.
-- Flyway runs this file once and records it in its schema history table.
-- Keep future schema changes in new V2, V3, ... migration files rather than
-- editing this file after it has been applied to a shared database.

-- The service catalogue is referenced by bookings and holds.
CREATE TABLE services (
    id VARCHAR(80) PRIMARY KEY,
    name VARCHAR(160) NOT NULL,
    duration_minutes INTEGER NOT NULL CHECK (duration_minutes > 0),
    price_zar INTEGER NOT NULL CHECK (price_zar >= 0),
    requires_approval BOOLEAN NOT NULL DEFAULT FALSE,
    active BOOLEAN NOT NULL DEFAULT TRUE
);

-- One row represents the normal opening window for one ISO weekday.
-- ISO numbering: Monday = 1, Tuesday = 2, ..., Sunday = 7.
CREATE TABLE business_hours (
    id BIGSERIAL PRIMARY KEY,
    day_of_week SMALLINT NOT NULL CHECK (day_of_week BETWEEN 1 AND 7),
    opens_at TIME NOT NULL,
    closes_at TIME NOT NULL,
    UNIQUE (day_of_week),
    CHECK (opens_at < closes_at)
);

-- Breaks, leave, maintenance, and manually blocked time live here.
CREATE TABLE blocked_periods (
    id BIGSERIAL PRIMARY KEY,
    starts_at TIMESTAMP NOT NULL,
    ends_at TIMESTAMP NOT NULL,
    reason VARCHAR(255) NOT NULL,
    CHECK (starts_at < ends_at)
);

-- The final conflict check will compare starts_at/ends_at against this table.
-- Cancelled records remain for audit history but are ignored by availability.
CREATE TABLE bookings (
    id BIGSERIAL PRIMARY KEY,
    booking_reference VARCHAR(40) NOT NULL UNIQUE,
    service_id VARCHAR(80) NOT NULL REFERENCES services(id),
    client_name VARCHAR(120) NOT NULL,
    phone VARCHAR(30) NOT NULL,
    email VARCHAR(255),
    starts_at TIMESTAMP NOT NULL,
    ends_at TIMESTAMP NOT NULL,
    -- These dimensions are intentionally separate: approval and payment can
    -- progress independently while the booking itself moves through its lifecycle.
    booking_status VARCHAR(40) NOT NULL DEFAULT 'PENDING',
    approval_status VARCHAR(40) NOT NULL DEFAULT 'NOT_STARTED',
    payment_status VARCHAR(40) NOT NULL DEFAULT 'NOT_STARTED',
    source VARCHAR(30) NOT NULL DEFAULT 'website',
    notes TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CHECK (starts_at < ends_at)
);

-- Temporary holds protect a slot while the client completes payment or approval.
-- Expired holds must be ignored or removed by the hold service.
CREATE TABLE booking_holds (
    id BIGSERIAL PRIMARY KEY,
    hold_reference VARCHAR(40) NOT NULL UNIQUE,
    service_id VARCHAR(80) NOT NULL REFERENCES services(id),
    starts_at TIMESTAMP NOT NULL,
    ends_at TIMESTAMP NOT NULL,
    expires_at TIMESTAMP NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CHECK (starts_at < ends_at),
    CHECK (created_at < expires_at)
);

CREATE INDEX idx_blocked_periods_range ON blocked_periods (starts_at, ends_at);
CREATE INDEX idx_bookings_range ON bookings (starts_at, ends_at);
CREATE INDEX idx_bookings_booking_status ON bookings (booking_status);
CREATE INDEX idx_bookings_approval_status ON bookings (approval_status);
CREATE INDEX idx_bookings_payment_status ON bookings (payment_status);
CREATE INDEX idx_booking_holds_range ON booking_holds (starts_at, ends_at, expires_at);

-- Seed the current service catalogue.
INSERT INTO services (id, name, duration_minutes, price_zar, requires_approval) VALUES
    ('basic-manicure', 'Basic manicure', 45, 250, FALSE),
    ('gel-overlay', 'Gel overlay', 60, 300, FALSE),
    ('gel-overlay-art', 'Gel overlay with nail art', 120, 450, TRUE),
    ('acrylic-extensions', 'Acrylic extensions', 150, 600, TRUE),
    ('not-sure', 'Not sure yet', 60, 0, TRUE);

-- ISO day numbers: Monday = 1, Sunday = 7. Sunday and Monday are closed.
INSERT INTO business_hours (day_of_week, opens_at, closes_at) VALUES
    (2, '08:00', '17:00'),
    (3, '08:00', '17:00'),
    (4, '08:00', '17:00'),
    (5, '08:00', '17:00'),
    (6, '08:00', '17:00');
