-- Hibernate's ddl-auto=update does not remove columns that disappeared from an entity.
-- The hold expiration is now maintained only by Redis key TTLs.
ALTER TABLE IF EXISTS bookings DROP COLUMN IF EXISTS hold_expires_at;

