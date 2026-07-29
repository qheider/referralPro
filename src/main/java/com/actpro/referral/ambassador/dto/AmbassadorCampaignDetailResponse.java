package com.actpro.referral.ambassador.dto;

import com.actpro.referral.campaign.CampaignStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record AmbassadorCampaignDetailResponse(
        Long assignmentId,
        Long campaignId,
        String campaignName,
        String description,
        String landingPageUrl,
        CampaignStatus status,
        LocalDateTime startDate,
        LocalDateTime endDate,
        String conversionEventName,
        String rewardType,
        BigDecimal referrerRewardValue,
        BigDecimal refereeRewardValue,
        long clickCount,
        long registrationCount,
        long bookingStartedCount,
        long completedRentalCount,
        BigDecimal registrationConversionRate,
        BigDecimal rentalConversionRate,
        ReferralLinkSummaryResponse referralLink
) {
}
