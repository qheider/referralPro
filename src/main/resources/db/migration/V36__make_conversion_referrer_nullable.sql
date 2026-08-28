-- Ambassador-driven conversions (ConversionService.completeConversion) have no PlatformUser
-- referrer - only a DashboardUser ambassador, rewarded via revenue.AmbassadorReward instead of
-- reward.Reward. Loosening this FK's NOT NULL is additive/non-destructive (no data loss, no
-- existing row touched, every row written so far already has a non-null referrer_user_id);
-- RewardService/ConversionService still always set it for the legacy direct-API flow.

ALTER TABLE conversions
    MODIFY COLUMN referrer_user_id BIGINT NULL;
