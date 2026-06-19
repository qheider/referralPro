CREATE TABLE referral_clicks (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    referral_id BIGINT NOT NULL,
    ip_address VARCHAR(50),
    user_agent VARCHAR(500),
    clicked_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (referral_id) REFERENCES referrals(id) ON DELETE CASCADE,
    INDEX idx_referral_clicks_referral_id (referral_id),
    INDEX idx_referral_clicks_clicked_at (clicked_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
