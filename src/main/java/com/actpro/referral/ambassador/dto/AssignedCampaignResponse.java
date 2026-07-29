package com.actpro.referral.ambassador.dto;

import com.actpro.referral.ambassador.AssignmentStatus;
import com.actpro.referral.campaign.CampaignStatus;

import java.time.LocalDateTime;

public record AssignedCampaignResponse(
        Long assignmentId,
        Long campaignId,
        String campaignName,
        CampaignStatus campaignStatus,
        LocalDateTime startDate,
        LocalDateTime endDate,
        AssignmentStatus assignmentStatus,
        LocalDateTime assignedAt,
        ReferralLinkSummaryResponse referralLink
) {
}
