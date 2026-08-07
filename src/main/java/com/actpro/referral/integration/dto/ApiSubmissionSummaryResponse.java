package com.actpro.referral.integration.dto;

import com.actpro.referral.integration.ApiSubmissionStatus;

import java.time.LocalDateTime;

public record ApiSubmissionSummaryResponse(
        Long id,
        String externalRequestId,
        ApiSubmissionStatus status,
        int attempts,
        int maxAttempts,
        Long referralId,
        String lastError,
        LocalDateTime availableAt,
        LocalDateTime submittedAt,
        LocalDateTime createdAt
) {
}
