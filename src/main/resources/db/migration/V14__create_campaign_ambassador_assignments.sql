CREATE TABLE campaign_ambassador_assignments (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    company_id BIGINT NOT NULL,
    campaign_id BIGINT NOT NULL,
    ambassador_user_id BIGINT NOT NULL,
    assigned_by_user_id BIGINT NULL,
    assigned_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    status VARCHAR(50) NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_campaign_ambassador_assignments_company FOREIGN KEY (company_id) REFERENCES companies(id) ON DELETE CASCADE,
    CONSTRAINT fk_campaign_ambassador_assignments_campaign FOREIGN KEY (campaign_id) REFERENCES campaigns(id) ON DELETE CASCADE,
    CONSTRAINT fk_campaign_ambassador_assignments_ambassador FOREIGN KEY (ambassador_user_id) REFERENCES dashboard_users(id) ON DELETE CASCADE,
    CONSTRAINT fk_campaign_ambassador_assignments_assigned_by FOREIGN KEY (assigned_by_user_id) REFERENCES dashboard_users(id) ON DELETE SET NULL,
    CONSTRAINT uk_campaign_ambassador_assignments_campaign_user UNIQUE (campaign_id, ambassador_user_id),
    INDEX idx_campaign_ambassador_assignments_company_id (company_id),
    INDEX idx_campaign_ambassador_assignments_ambassador_user_id (ambassador_user_id),
    INDEX idx_campaign_ambassador_assignments_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
