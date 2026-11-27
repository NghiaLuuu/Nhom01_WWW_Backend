-- Additive migration only: leave cancel_requests/bookings/tickets/payments unchanged.
CREATE TABLE IF NOT EXISTS cancel_request_workflow_details (
    cancel_request_id bigint PRIMARY KEY REFERENCES cancel_requests(id),
    request_reason text,
    submitted_by_email varchar(255),
    source varchar(20) NOT NULL,
    submitted_at timestamp NOT NULL,
    decision_note text,
    processed_at timestamp,
    version bigint NOT NULL DEFAULT 0
);
