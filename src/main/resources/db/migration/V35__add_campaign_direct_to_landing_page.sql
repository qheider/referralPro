-- Lets a company admin skip ReferralPro's own /r/{token} redirect + /refer/{token} lead-capture
-- page for ambassador links entirely: when enabled (and landingPageUrl is non-blank), an
-- ambassador's referral link/QR point straight at the company's landing page with a ?ref= query
-- param, and the company calls back via POST /api/conversions to report registration. Additive,
-- defaults to today's existing behavior (disabled). See Campaign.isDirectToLandingPageMode(),
-- CampaignAssignmentService/AmbassadorPortalService (link/QR generation) and ConversionService
-- (conversion-by-token handling).

ALTER TABLE campaigns
    ADD COLUMN direct_to_landing_page_enabled BOOLEAN NOT NULL DEFAULT FALSE AFTER landing_page_url;
