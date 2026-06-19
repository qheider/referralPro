package com.actpro.referral.campaign.dto;

import com.actpro.referral.campaign.RewardType;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record CreateCampaignRequest(
        @NotBlank(message = "Campaign name is required")
        String name,

        String description,

        @NotBlank(message = "Landing page URL is required")
        String landingPageUrl,

        @NotNull(message = "Start date is required")
        LocalDateTime startDate,

        @NotNull(message = "End date is required")
        LocalDateTime endDate,

        @NotNull(message = "Reward type is required")
        RewardType rewardType,

        @NotNull(message = "Referrer reward value is required")
        @DecimalMin(value = "0.0", message = "Referrer reward value must be positive")
        BigDecimal referrerRewardValue,

        @NotNull(message = "Referee reward value is required")
        @DecimalMin(value = "0.0", message = "Referee reward value must be positive")
        BigDecimal refereeRewardValue,

        @NotBlank(message = "Conversion event name is required")
        String conversionEventName
) {
}
