-- Hibernate's ddl-auto=update does not remove columns that disappeared from an entity.
-- The hold expiration is now maintained only by Redis key TTLs.
ALTER TABLE IF EXISTS bookings DROP COLUMN IF EXISTS hold_expires_at;

-- Ensure tickets table has created_at and updated_at for TicketNoShowScheduler
ALTER TABLE IF EXISTS tickets ADD COLUMN IF NOT EXISTS created_at TIMESTAMP(6);
ALTER TABLE IF EXISTS tickets ADD COLUMN IF NOT EXISTS updated_at TIMESTAMP(6);
