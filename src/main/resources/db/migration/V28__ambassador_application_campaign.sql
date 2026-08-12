-- Links an ambassador application to the campaign the applicant applied through via the Phase 3
-- public join link (/api/campaigns/join/{campaignCode}). Nullable: the existing company-wide
-- admin-invited application path (no campaign context) remains valid.
ALTER TABLE ambassador_applications
    ADD COLUMN campaign_id BIGINT NULL AFTER company_id,
    ADD CONSTRAINT fk_ambassador_applications_campaign FOREIGN KEY (campaign_id) REFERENCES campaigns(id) ON DELETE SET NULL,
    ADD INDEX idx_ambassador_applications_campaign_id (campaign_id);
