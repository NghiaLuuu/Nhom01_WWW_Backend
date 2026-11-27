-- Schema initialization migrations for PostgreSQL
-- Note: DDL operations that require exclusive table locks should not run repeatedly on every application startup.
-- hold_expires_at, tickets.created_at, and tickets.updated_at have been verified.

CREATE INDEX IF NOT EXISTS idx_bookings_trip_id ON bookings(trip_id);
CREATE INDEX IF NOT EXISTS idx_bookings_customer_id ON bookings(customer_id);
CREATE INDEX IF NOT EXISTS idx_bookings_status ON bookings(status);
CREATE INDEX IF NOT EXISTS idx_bookings_created_at ON bookings(created_at DESC);

CREATE INDEX IF NOT EXISTS idx_tickets_booking_id ON tickets(booking_id);
CREATE INDEX IF NOT EXISTS idx_tickets_seat_id ON tickets(seat_id);
CREATE INDEX IF NOT EXISTS idx_tickets_status ON tickets(status);
CREATE INDEX IF NOT EXISTS idx_tickets_booking_status ON tickets(booking_id, status);

CREATE INDEX IF NOT EXISTS idx_seats_vehicle_id ON seats(vehicle_id);
CREATE INDEX IF NOT EXISTS idx_trips_status ON trips(status);

SELECT 1;
