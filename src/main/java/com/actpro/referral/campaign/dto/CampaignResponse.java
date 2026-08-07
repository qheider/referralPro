package com.actpro.referral.campaign.dto;

import com.actpro.referral.campaign.CampaignStatus;
import com.actpro.referral.campaign.RewardType;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record CampaignResponse(
        Long campaignId,
        String campaignCode,
        String joinLink,
        String name,
        String description,
        String landingPageUrl,
        LocalDateTime startDate,
        LocalDateTime endDate,
        LocalDateTime ambassadorEnrollmentStart,
        LocalDateTime ambassadorEnrollmentEnd,
        RewardType rewardType,
        BigDecimal referrerRewardValue,
        BigDecimal refereeRewardValue,
        String conversionEventName,
        CampaignStatus status,
        LocalDateTime createdAt
) {
}
