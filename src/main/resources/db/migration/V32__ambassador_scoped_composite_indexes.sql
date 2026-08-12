-- Phase 9 (hardening and release readiness): index/query performance review finding. Every
-- AmbassadorPortalService method (dashboard, campaign list/detail, referral history, analytics -
-- i.e. every single ambassador-portal page load) filters referrals/referral_clicks/
-- campaign_ambassador_assignments by (ambassador_user_id, company_id), sometimes further narrowed
-- by campaign_id/status/date range (see ReferralRepository/ReferralClickRepository/
-- CampaignAmbassadorAssignmentRepository's countBy.../findBy... methods). V14/V18 only ever added
-- a single-column index on ambassador_user_id for these three tables - correct for company_id
-- tenant-scoping, per CLAUDE.md's indexing convention, but not for the actual two-column filter
-- these queries run. Same fix already applied proactively to revenue_events/ambassador_rewards in
-- V31 (idx_revenue_events_ambassador/idx_ambassador_rewards_ambassador); this migration closes the
-- equivalent gap in the three older, higher-volume tables those queries were modeled on.
ALTER TABLE referrals
    ADD INDEX idx_referrals_ambassador_company (ambassador_user_id, company_id);

ALTER TABLE referral_clicks
    ADD INDEX idx_referral_clicks_ambassador_company (ambassador_user_id, company_id);

ALTER TABLE campaign_ambassador_assignments
    ADD INDEX idx_campaign_ambassador_assignments_ambassador_company (ambassador_user_id, company_id);
