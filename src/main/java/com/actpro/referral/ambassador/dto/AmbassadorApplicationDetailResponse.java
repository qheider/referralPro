package com.actpro.referral.ambassador.dto;

import com.actpro.referral.ambassador.ApplicationStatus;

import java.time.LocalDateTime;

public record AmbassadorApplicationDetailResponse(
        Long id,
        String firstName,
        String lastName,
        String email,
        String phone,
        String displayName,
        String bio,
        String socialMediaPlatform,
        String socialMediaHandle,
        ApplicationStatus status,
        String rejectionReason,
        Long reviewedByUserId,
        LocalDateTime reviewedAt,
        Long resultingAmbassadorProfileId,
        LocalDateTime submittedAt
) {
}
