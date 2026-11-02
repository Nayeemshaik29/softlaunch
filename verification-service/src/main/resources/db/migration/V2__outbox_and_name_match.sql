-- Soft signal for moderators: does the display name appear in the legal name?
ALTER TABLE verifications ADD COLUMN name_matched BOOLEAN;

-- Transactional outbox: written in the SAME transaction as the verification,
-- then published to Kafka by a background job. Kafka down = rows wait, nothing lost.
CREATE TABLE outbox_events (
    id           UUID          PRIMARY KEY,
    topic        VARCHAR(100)  NOT NULL,
    message_key  VARCHAR(100)  NOT NULL,
    payload      VARCHAR(4000) NOT NULL,
    created_at   TIMESTAMPTZ   NOT NULL,
    published_at TIMESTAMPTZ,
    attempts     INT           NOT NULL DEFAULT 0
);

-- small, fast index: only rows still waiting to be sent
CREATE INDEX idx_outbox_unpublished ON outbox_events (created_at) WHERE published_at IS NULL;
