package com.actpro.referral.campaign.dto;

import com.actpro.referral.campaign.RewardType;
import jakarta.validation.constraints.DecimalMin;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Partial update - every field is optional, only non-null values are applied. Reward-related
 * fields (rewardType, referrerRewardValue, refereeRewardValue, conversionEventName) and startDate
 * can only be changed while the campaign is still DRAFT: CampaignService enforces this so a
 * published campaign's financial terms can't be rewritten out from under rewards already
 * calculated against them (RewardService reads these fields live off Campaign - there is no
 * reward-rule snapshot table yet, see phases_tracker.txt Phase 3/8).
 */
public record UpdateCampaignRequest(
        String name,
        String description,
        String landingPageUrl,
        LocalDateTime startDate,
        LocalDateTime endDate,
        LocalDateTime ambassadorEnrollmentStart,
        LocalDateTime ambassadorEnrollmentEnd,
        RewardType rewardType,

        @DecimalMin(value = "0.0", message = "Referrer reward value must be positive")
        BigDecimal referrerRewardValue,

        @DecimalMin(value = "0.0", message = "Referee reward value must be positive")
        BigDecimal refereeRewardValue,

        String conversionEventName
) {
}
