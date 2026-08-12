-- Campaign lifecycle (Phase 3 of the Luup implementation plan): a public, non-sequential join
-- code/link, and a separate ambassador-enrollment window from the customer-referral window
-- (start_date/end_date keep their existing meaning - the customer-referral window - so existing
-- rows and isActive() semantics are unaffected).

ALTER TABLE campaigns
    ADD COLUMN campaign_code VARCHAR(20) NULL AFTER id,
    ADD COLUMN ambassador_enrollment_start TIMESTAMP NULL AFTER end_date,
    ADD COLUMN ambassador_enrollment_end TIMESTAMP NULL AFTER ambassador_enrollment_start;

-- Backfill existing rows: enrollment window defaults to the referral window (today's single-window
-- behavior), and campaign_code is derived deterministically from id/created_at so it's stable and
-- collision-free without needing per-row random generation in SQL. New campaigns get a real random
-- code from CampaignCodeGenerator.
UPDATE campaigns
SET ambassador_enrollment_start = start_date,
    ambassador_enrollment_end = end_date
WHERE ambassador_enrollment_start IS NULL;

UPDATE campaigns
SET campaign_code = UPPER(SUBSTRING(SHA2(CONCAT('campaign-', id, '-', UNIX_TIMESTAMP(created_at)), 256), 1, 10))
WHERE campaign_code IS NULL;

-- Pre-existing CANCELLED rows (if any) map to the new CLOSED status - see CampaignStatus, which
-- drops CANCELLED in favor of the target plan's CLOSED/ARCHIVED terminal states.
UPDATE campaigns SET status = 'CLOSED' WHERE status = 'CANCELLED';

ALTER TABLE campaigns
    MODIFY COLUMN campaign_code VARCHAR(20) NOT NULL,
    MODIFY COLUMN ambassador_enrollment_start TIMESTAMP NOT NULL,
    MODIFY COLUMN ambassador_enrollment_end TIMESTAMP NOT NULL,
    ADD CONSTRAINT uk_campaigns_campaign_code UNIQUE (campaign_code);
