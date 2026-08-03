-- Replace plaintext Company.api_key with a hashed, revocable, rotatable key lifecycle.
-- See docs/luup/CURRENT_STATE_ASSESSMENT.md section 4.3.
--
-- Split from the column drop (V21) so a failure partway through backfill leaves companies.api_key
-- intact and Flyway stops here for inspection, instead of a single non-transactional DDL/DML/DDL
-- script potentially failing between the CREATE TABLE and the irreversible DROP COLUMN.

CREATE TABLE company_api_keys (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    company_id BIGINT NOT NULL,
    key_id VARCHAR(40) NOT NULL,
    secret_hash VARCHAR(64) NOT NULL,
    secret_preview VARCHAR(12) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    expires_at TIMESTAMP NULL,
    last_used_at TIMESTAMP NULL,
    revoked_at TIMESTAMP NULL,
    rotated_from_key_id BIGINT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_company_api_keys_company FOREIGN KEY (company_id) REFERENCES companies(id) ON DELETE CASCADE,
    CONSTRAINT fk_company_api_keys_rotated_from FOREIGN KEY (rotated_from_key_id) REFERENCES company_api_keys(id),
    UNIQUE KEY uk_company_api_keys_key_id (key_id),
    UNIQUE KEY uk_company_api_keys_secret_hash (secret_hash),
    INDEX idx_company_api_keys_company_id (company_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Backfill one ACTIVE key per existing company from its current plaintext key, so existing
-- integrations (e.g. Luup) keep authenticating with the same key value after this migration -
-- only its server-side storage changes, from plaintext to a SHA-256 hash. This intentionally does
-- NOT force rotation: any prior exposure of the plaintext value (e.g. old application logs, DB
-- backups predating this fix) is not remediated by hashing it after the fact. That is an accepted,
-- disclosed tradeoff for this phase - forcing rotation would silently break existing integrations
-- (e.g. Luup) without out-of-band coordination. A rotation nudge to companies is a recommended
-- fast-follow, not something this migration can safely do unilaterally.
INSERT INTO company_api_keys (company_id, key_id, secret_hash, secret_preview, status, created_at, updated_at)
SELECT id, CONCAT('legacy_', id), SHA2(api_key, 256), RIGHT(api_key, 4), 'ACTIVE', created_at, updated_at
FROM companies;
