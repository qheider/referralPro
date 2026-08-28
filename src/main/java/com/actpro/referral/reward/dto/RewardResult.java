package com.actpro.referral.reward.dto;

import com.actpro.referral.revenue.AmbassadorReward;
import com.actpro.referral.reward.Reward;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RewardResult {
    // Legacy direct-API flow only - null for an ambassador-driven conversion (no PlatformUser
    // referrer to reward this way; see ambassadorReward below instead).
    private Reward referrerReward;
    // Always set, regardless of flow - the referee is always a real PlatformUser.
    private Reward refereeReward;
    // Ambassador-driven flow only - null for the legacy direct-API flow. See
    // RevenueEventService#recordConversionQualifyingEvent.
    private AmbassadorReward ambassadorReward;
}
