-- One-time invitation tokens for account setup (starting with ambassador onboarding), replacing
-- the undeliverable temp-password flow. See docs/luup/CURRENT_STATE_ASSESSMENT.md section 4.4.
CREATE TABLE account_invitations (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    dashboard_user_id BIGINT NOT NULL,
    company_id BIGINT NOT NULL,
    purpose VARCHAR(50) NOT NULL,
    token_hash VARCHAR(64) NOT NULL,
    expires_at TIMESTAMP NOT NULL,
    accepted_at TIMESTAMP NULL,
    revoked_at TIMESTAMP NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_account_invitations_dashboard_user FOREIGN KEY (dashboard_user_id) REFERENCES dashboard_users(id) ON DELETE CASCADE,
    CONSTRAINT fk_account_invitations_company FOREIGN KEY (company_id) REFERENCES companies(id) ON DELETE CASCADE,
    UNIQUE KEY uk_account_invitations_token_hash (token_hash),
    INDEX idx_account_invitations_dashboard_user_id (dashboard_user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
