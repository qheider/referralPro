package com.actpro.referral.integration.dto;

import com.actpro.referral.integration.AttemptOutcome;
import com.actpro.referral.integration.FailureCategory;

import java.time.LocalDateTime;

public record IntegrationAttemptResponse(
        int attemptNumber,
        LocalDateTime startedAt,
        LocalDateTime completedAt,
        Integer httpStatus,
        AttemptOutcome outcome,
        FailureCategory failureCategory,
        String sanitizedMessage,
        LocalDateTime nextRetryAt
) {
}
