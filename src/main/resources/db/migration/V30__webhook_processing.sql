-- Phase 7 (webhook processing): activates the two Phase-6-seeded-but-inert company_integrations
-- columns (webhook_signing_secret, status_mapping_json) by adding webhook_public_id - the
-- "non-guessable public identifier" the plan requires for resolving a company from an inbound
-- webhook URL. Deliberately a separate identifier from campaigns.campaign_code or company id, so
-- a leaked webhook URL doesn't expose anything used elsewhere. Also adds webhook_events, the
-- durable inbound-record table.

ALTER TABLE company_integrations
    ADD COLUMN webhook_public_id VARCHAR(20) NULL AFTER id;

-- Backfill existing rows deterministically (same SHA2-substring technique V27 used for
-- campaign_code) so this can be NOT NULL UNIQUE without per-row random generation in SQL. New
-- rows (companies registering after this migration) get a real random id from
-- WebhookPublicIdGenerator at CompanyService.registerCompany's existing seed point.
UPDATE company_integrations
SET webhook_public_id = UPPER(SUBSTRING(SHA2(CONCAT('webhook-', id, '-', UNIX_TIMESTAMP(created_at)), 256), 1, 16))
WHERE webhook_public_id IS NULL;

ALTER TABLE company_integrations
    MODIFY COLUMN webhook_public_id VARCHAR(20) NOT NULL,
    ADD CONSTRAINT uk_company_integrations_webhook_public_id UNIQUE (webhook_public_id);

CREATE TABLE webhook_events (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    company_id BIGINT NOT NULL,
    event_id VARCHAR(100) NOT NULL,
    event_type VARCHAR(100) NOT NULL,
    status VARCHAR(30) NOT NULL DEFAULT 'RECEIVED',
    -- Exact verified request body, stored before any business parsing - the durable record the
    -- plan requires, independent of whether later reference-matching/status-mapping succeeds.
    raw_payload TEXT NOT NULL,
    matched_referral_id BIGINT NULL,
    mapped_status VARCHAR(30) NULL,
    attempts INT NOT NULL DEFAULT 0,
    max_attempts INT NOT NULL,
    available_at TIMESTAMP NOT NULL,
    locked_by VARCHAR(64) NULL,
    failure_reason VARCHAR(2000) NULL,
    processed_at TIMESTAMP NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_webhook_events_company FOREIGN KEY (company_id) REFERENCES companies(id) ON DELETE CASCADE,
    -- ON DELETE SET NULL (not CASCADE, unlike the company FK above): a webhook event record
    -- shouldn't disappear if the matched referral is later deleted, just lose the link.
    CONSTRAINT fk_webhook_events_referral FOREIGN KEY (matched_referral_id) REFERENCES referrals(id) ON DELETE SET NULL,
    -- Requirement: enforce uniqueness of (company_id, eventId) - the redelivery guard.
    CONSTRAINT uk_webhook_events_company_event UNIQUE (company_id, event_id),
    INDEX idx_webhook_events_company_id (company_id),
    -- Supports WebhookDispatcher's claim query, same shape as idx_api_submissions_dispatch.
    INDEX idx_webhook_events_dispatch (status, available_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
