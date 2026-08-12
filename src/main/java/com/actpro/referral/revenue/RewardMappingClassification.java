package com.actpro.referral.revenue;

/**
 * How a mapped {@link com.actpro.referral.referral.ReferralStatus} should be treated by
 * {@link RevenueEventService} once {@code ReferralStatusMappingService} has already applied it to
 * a referral. Resolved by {@link RewardStatusMappingService} from the company's
 * {@code CompanyIntegration.rewardMappingJson} (default policy if unset/unmapped - see that
 * class).
 */
public enum RewardMappingClassification {
    // Creates (or is a no-op idempotent redelivery against) a RevenueEvent + AmbassadorReward.
    QUALIFYING,
    // Reverses a previously-recorded RevenueEvent + its AmbassadorReward, if one exists.
    REVERSING,
    // No revenue/reward consequence.
    IGNORE
}
