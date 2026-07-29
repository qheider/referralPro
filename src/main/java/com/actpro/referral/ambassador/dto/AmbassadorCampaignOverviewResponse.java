package com.actpro.referral.ambassador.dto;

import com.actpro.referral.campaign.CampaignStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record AmbassadorCampaignOverviewResponse(
        Long assignmentId,
        Long campaignId,
        String campaignName,
        String description,
        CampaignStatus status,
        LocalDateTime startDate,
        LocalDateTime endDate,
        String conversionEventName,
        BigDecimal referrerRewardValue,
        BigDecimal refereeRewardValue,
        String rewardType,
        long clickCount,
        long registrationCount,
        long completedRentalCount,
        BigDecimal registrationConversionRate,
        ReferralLinkSummaryResponse referralLink
) {
}
