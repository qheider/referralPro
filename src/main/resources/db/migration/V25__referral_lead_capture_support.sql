-- Ambassador-driven leads have no PlatformUser referrer (the referrer is the ambassador,
-- already tracked via ambassador_user_id) - relax the legacy-flow-only NOT NULL assumption.
-- AmbassadorPortalService.searchByAmbassador/findRecentByAmbassador already LEFT JOIN
-- referrer_user, confirming this was anticipated.
ALTER TABLE referrals
    MODIFY COLUMN referrer_user_id BIGINT NULL;

-- Attribution-session linkage: correlates a submitted lead back to the browsing session that
-- clicked the referral link (rp_attr_session cookie, see ReferralRedirectController), enabling
-- idempotent handling of a same-session resubmission.
ALTER TABLE referrals
    ADD COLUMN attribution_session_id VARCHAR(100) NULL AFTER referral_link_id,
    ADD INDEX idx_referrals_link_session (referral_link_id, attribution_session_id);
