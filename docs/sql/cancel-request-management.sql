-- Optional manual migration for an existing PostgreSQL database.
-- Review/backup before applying. Not run automatically by this project.
BEGIN;
ALTER TABLE cancel_requests ADD COLUMN IF NOT EXISTS reason VARCHAR(2000);
ALTER TABLE cancel_requests ALTER COLUMN reject_reason TYPE VARCHAR(2000);
-- Legacy code stored the submitted reason in reject_reason.
-- For previously rejected requests, the original reason cannot be recovered.
UPDATE cancel_requests
SET reason = reject_reason
WHERE reason IS NULL AND status IN ('PENDING', 'APPROVED');
COMMIT;
