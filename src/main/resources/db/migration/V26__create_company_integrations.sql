-- Minimal CompanyIntegration stub: seeds a NOT_CONFIGURED row per company at registration time
-- (Section 3 of the Luup implementation plan). Only status is tracked here - the full config
-- surface (API URL, auth type, encrypted credentials, retry policy, webhook signing, status
-- mapping) is Phase 6 scope and will extend this table with a later migration rather than
-- reshape it.
CREATE TABLE company_integrations (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    company_id BIGINT NOT NULL,
    status VARCHAR(30) NOT NULL DEFAULT 'NOT_CONFIGURED',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_company_integrations_company FOREIGN KEY (company_id) REFERENCES companies(id) ON DELETE CASCADE,
    UNIQUE KEY uk_company_integrations_company_id (company_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
