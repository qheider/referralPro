package com.actpro.referral.ambassador.dto;

import com.actpro.referral.ambassador.ApplicationStatus;

import java.time.LocalDateTime;

public record AmbassadorApplicationSubmissionResponse(
        Long applicationId,
        ApplicationStatus status,
        LocalDateTime submittedAt
) {
}
