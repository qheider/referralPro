package com.actpro.referral.ambassador.dto;

import com.actpro.referral.ambassador.ApplicationStatus;

import java.time.LocalDateTime;

public record AmbassadorApplicationSummaryResponse(
        Long id,
        String firstName,
        String lastName,
        String email,
        String displayName,
        Long campaignId,
        String campaignName,
        ApplicationStatus status,
        LocalDateTime submittedAt,
        LocalDateTime reviewedAt
) {
}
