ALTER TABLE referral_clicks
    ADD COLUMN company_id BIGINT NULL AFTER referral_id,
    ADD COLUMN campaign_id BIGINT NULL AFTER company_id,
    ADD COLUMN referral_link_id BIGINT NULL AFTER campaign_id,
    ADD COLUMN ambassador_user_id BIGINT NULL AFTER referral_link_id,
    ADD COLUMN session_id VARCHAR(100) NULL AFTER ambassador_user_id,
    ADD COLUMN ip_hash VARCHAR(255) NULL AFTER ip_address,
    ADD COLUMN referrer_url VARCHAR(1000) NULL AFTER user_agent;

ALTER TABLE referral_clicks
    ADD CONSTRAINT fk_referral_clicks_company FOREIGN KEY (company_id) REFERENCES companies(id) ON DELETE SET NULL,
    ADD CONSTRAINT fk_referral_clicks_campaign FOREIGN KEY (campaign_id) REFERENCES campaigns(id) ON DELETE SET NULL,
    ADD CONSTRAINT fk_referral_clicks_referral_link FOREIGN KEY (referral_link_id) REFERENCES referral_links(id) ON DELETE SET NULL,
    ADD CONSTRAINT fk_referral_clicks_ambassador FOREIGN KEY (ambassador_user_id) REFERENCES dashboard_users(id) ON DELETE SET NULL;
