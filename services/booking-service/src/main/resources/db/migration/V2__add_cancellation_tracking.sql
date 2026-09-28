ALTER TABLE bookings
    ADD COLUMN refund_status VARCHAR(40) NOT NULL DEFAULT 'NOT_APPLICABLE',
    ADD COLUMN cancellation_reason TEXT;

CREATE INDEX idx_bookings_refund_status ON bookings (refund_status);
