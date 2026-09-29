ALTER TABLE bookings
    ADD COLUMN calendar_event_id VARCHAR(255);

CREATE UNIQUE INDEX uq_bookings_calendar_event_id
    ON bookings (calendar_event_id)
    WHERE calendar_event_id IS NOT NULL;
