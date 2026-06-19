CREATE TABLE referrals (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    company_id BIGINT NOT NULL,
    campaign_id BIGINT NOT NULL,
    referrer_user_id BIGINT NOT NULL,
    referral_code VARCHAR(50) NOT NULL,
    referral_link VARCHAR(500) NOT NULL,
    status VARCHAR(50) NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (company_id) REFERENCES companies(id) ON DELETE CASCADE,
    FOREIGN KEY (campaign_id) REFERENCES campaigns(id) ON DELETE CASCADE,
    FOREIGN KEY (referrer_user_id) REFERENCES platform_users(id) ON DELETE CASCADE,
    UNIQUE KEY uk_referrals_code (referral_code),
    INDEX idx_referrals_company_id (company_id),
    INDEX idx_referrals_campaign_id (campaign_id),
    INDEX idx_referrals_referrer_user_id (referrer_user_id),
    INDEX idx_referrals_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
