-- One-time password reset tokens for the "forgot password" flow, kept separate from
-- account_invitations (V22): reset links need a much shorter expiry than the 7-day invitation
-- window, and reissuing a reset token must not revoke an unrelated pending invitation for the
-- same user (account_invitations' revoke-on-reissue is purpose-agnostic).
CREATE TABLE password_reset_tokens (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    dashboard_user_id BIGINT NOT NULL,
    company_id BIGINT NOT NULL,
    token_hash VARCHAR(64) NOT NULL,
    expires_at TIMESTAMP NOT NULL,
    used_at TIMESTAMP NULL,
    revoked_at TIMESTAMP NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_password_reset_tokens_dashboard_user FOREIGN KEY (dashboard_user_id) REFERENCES dashboard_users(id) ON DELETE CASCADE,
    CONSTRAINT fk_password_reset_tokens_company FOREIGN KEY (company_id) REFERENCES companies(id) ON DELETE CASCADE,
    UNIQUE KEY uk_password_reset_tokens_token_hash (token_hash),
    INDEX idx_password_reset_tokens_dashboard_user_id (dashboard_user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
