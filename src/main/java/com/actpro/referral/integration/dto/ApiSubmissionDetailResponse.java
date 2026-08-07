package com.actpro.referral.integration.dto;

import com.actpro.referral.integration.ApiSubmissionStatus;

import java.time.LocalDateTime;
import java.util.List;

public record ApiSubmissionDetailResponse(
        Long id,
        String externalRequestId,
        ApiSubmissionStatus status,
        int attemptCount,
        int maxAttempts,
        Long referralId,
        String lastError,
        String companyCustomerReference,
        String companyTransactionReference,
        LocalDateTime availableAt,
        LocalDateTime submittedAt,
        LocalDateTime createdAt,
        List<IntegrationAttemptResponse> attempts
) {
}
