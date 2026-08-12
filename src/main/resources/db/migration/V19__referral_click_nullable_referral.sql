-- Ambassador referral-link clicks (public_token) have no Referral row until a lead
-- is later submitted against the link, so referral_id can no longer be mandatory.
ALTER TABLE referral_clicks
    MODIFY COLUMN referral_id BIGINT NULL,
    ALGORITHM=INPLACE, LOCK=NONE;

-- Dashboard analytics now read company_id/campaign_id/ambassador_user_id/referral_link_id
-- directly off referral_clicks instead of joining through referrals, so backfill those
-- columns (added nullable in V16 but never populated) on rows recorded before this change.
UPDATE referral_clicks rc
JOIN referrals r ON r.id = rc.referral_id
SET rc.company_id = r.company_id,
    rc.campaign_id = r.campaign_id,
    rc.ambassador_user_id = r.ambassador_user_id,
    rc.referral_link_id = r.referral_link_id
WHERE rc.company_id IS NULL;
