package com.actpro.referral.campaign.dto;

import com.actpro.referral.campaign.RewardType;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record CreateCampaignRequest(
        @NotBlank(message = "Campaign name is required")
        String name,

        String description,

        String qualifyingConditions,

        String incentiveDescription,

        String termsUrl,

        @DecimalMin(value = "0.0", message = "Budget cap must be positive")
        BigDecimal budgetCap,

        @NotBlank(message = "Landing page URL is required")
        String landingPageUrl,

        // When true (and landingPageUrl is non-blank), ambassador links/QR for this campaign point
        // straight at landingPageUrl instead of ReferralPro's own redirect + lead-capture page.
        // Defaults to false if omitted.
        boolean directToLandingPageEnabled,

        @NotNull(message = "Start date is required")
        LocalDateTime startDate,

        @NotNull(message = "End date is required")
        LocalDateTime endDate,

        @NotNull(message = "Ambassador enrollment start date is required")
        LocalDateTime ambassadorEnrollmentStart,

        @NotNull(message = "Ambassador enrollment end date is required")
        LocalDateTime ambassadorEnrollmentEnd,

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
