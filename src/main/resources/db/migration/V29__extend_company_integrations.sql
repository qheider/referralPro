-- Phase 6 (Company API integration): extends the V26 company_integrations stub in place with the
-- full config surface (API URL, auth, encrypted credentials, timeout, retry policy, last test
-- result), plus two forward-looking columns unused until later phases (webhook_signing_secret -
-- Phase 7, status_mapping_json/reward_mapping_json - Phase 7/8), and adds the two new tables that
-- track outgoing Create User API delivery: api_submissions (one row per outbound customer-create
-- attempt, idempotent per company+aggregate+event) and integration_attempts (one row per HTTP try).

ALTER TABLE company_integrations
    ADD COLUMN api_base_url VARCHAR(500) NULL,
    ADD COLUMN auth_type VARCHAR(30) NOT NULL DEFAULT 'NONE',
    ADD COLUMN encrypted_credentials TEXT NULL,
    ADD COLUMN request_timeout_ms INT NOT NULL DEFAULT 10000,
    ADD COLUMN max_retry_attempts INT NOT NULL DEFAULT 5,
    ADD COLUMN last_tested_at TIMESTAMP NULL,
    ADD COLUMN last_test_result VARCHAR(20) NULL,
    ADD COLUMN last_test_message VARCHAR(2000) NULL,
    -- Unused until Phase 7 (webhook signature verification) - encrypted like credentials above.
    ADD COLUMN webhook_signing_secret TEXT NULL,
    -- Unused until Phase 7 (status mapping) / Phase 8 (reward mapping) - opaque JSON, only
    -- syntax-validated on write by CompanyIntegrationService, not semantically interpreted yet.
    ADD COLUMN status_mapping_json TEXT NULL,
    ADD COLUMN reward_mapping_json TEXT NULL,
    -- Supports ApiSubmissionDispatchService's claim query (only dispatch for ACTIVE integrations).
    ADD INDEX idx_company_integrations_status (status);

CREATE TABLE api_submissions (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    company_id BIGINT NOT NULL,
    -- The domain entity this submission originated from, e.g. "REFERRAL" - mirrors outbox_events.
    aggregate_type VARCHAR(50) NOT NULL,
    aggregate_id BIGINT NOT NULL,
    -- The outbox event type that created this submission, e.g. "referral.lead_registered".
    source_event_type VARCHAR(100) NOT NULL,
    -- Idempotency key sent to the company's Create User API as externalRequestId.
    external_request_id VARCHAR(64) NOT NULL,
    status VARCHAR(30) NOT NULL DEFAULT 'PENDING',
    attempts INT NOT NULL DEFAULT 0,
    -- Snapshot of company_integrations.max_retry_attempts at creation time, so a later config
    -- change doesn't retroactively change the retry budget of an already-queued submission.
    max_attempts INT NOT NULL,
    available_at TIMESTAMP NOT NULL,
    locked_by VARCHAR(64) NULL,
    last_error VARCHAR(2000) NULL,
    company_customer_reference VARCHAR(255) NULL,
    company_transaction_reference VARCHAR(255) NULL,
    submitted_at TIMESTAMP NULL,
    last_response_status INT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_api_submissions_company FOREIGN KEY (company_id) REFERENCES companies(id) ON DELETE CASCADE,
    -- Idempotency guard: re-processing the same outbox row (e.g. after a crash) must never create
    -- a second submission for the same source event.
    CONSTRAINT uk_api_submissions_idempotency UNIQUE (company_id, aggregate_type, aggregate_id, source_event_type),
    CONSTRAINT uk_api_submissions_external_request_id UNIQUE (external_request_id),
    INDEX idx_api_submissions_company_id (company_id),
    INDEX idx_api_submissions_dispatch (status, available_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE integration_attempts (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    api_submission_id BIGINT NOT NULL,
    attempt_number INT NOT NULL,
    started_at TIMESTAMP NOT NULL,
    completed_at TIMESTAMP NULL,
    http_status INT NULL,
    outcome VARCHAR(20) NOT NULL,
    failure_category VARCHAR(30) NULL,
    sanitized_message VARCHAR(2000) NULL,
    next_retry_at TIMESTAMP NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_integration_attempts_submission FOREIGN KEY (api_submission_id) REFERENCES api_submissions(id) ON DELETE CASCADE,
    CONSTRAINT uk_integration_attempts_submission_attempt UNIQUE (api_submission_id, attempt_number),
    INDEX idx_integration_attempts_submission (api_submission_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
