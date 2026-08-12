package com.actpro.referral.ambassador.dto;

import com.actpro.referral.ambassador.AssignmentStatus;

import java.time.LocalDateTime;

public record CampaignAssignmentResponse(
        Long assignmentId,
        Long campaignId,
        Long ambassadorId,
        Long ambassadorUserId,
        String ambassadorFirstName,
        String ambassadorLastName,
        String ambassadorEmail,
        String displayName,
        AssignmentStatus status,
        LocalDateTime assignedAt,
        ReferralLinkSummaryResponse referralLink
) {
}
