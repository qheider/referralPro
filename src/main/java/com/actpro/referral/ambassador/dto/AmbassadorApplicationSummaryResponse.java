package com.actpro.referral.ambassador.dto;

import com.actpro.referral.ambassador.ApplicationStatus;

import java.time.LocalDateTime;

public record AmbassadorApplicationSummaryResponse(
        Long id,
        String firstName,
        String lastName,
        String email,
        String displayName,
        ApplicationStatus status,
        LocalDateTime submittedAt,
        LocalDateTime reviewedAt
) {
}
