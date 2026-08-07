package com.actpro.referral.integration.dto;

import com.actpro.referral.company.CompanyIntegrationStatus;
import com.actpro.referral.integration.IntegrationAuthType;

import java.time.LocalDateTime;

/**
 * Never carries a raw or decrypted credential value - only {@code hasCredentials}. Decrypting
 * stored credentials happens only in outbound-call paths (CreateUserApiClient, by way of
 * ApiSubmissionDispatchService/CompanyIntegrationService#testConnection), never to satisfy a
 * config-read request.
 */
public record CompanyIntegrationConfigResponse(
        Long id,
        CompanyIntegrationStatus status,
        String apiBaseUrl,
        IntegrationAuthType authType,
        boolean hasCredentials,
        int requestTimeoutMs,
        int maxRetryAttempts,
        String statusMappingJson,
        String rewardMappingJson,
        LocalDateTime lastTestedAt,
        String lastTestResult,
        String lastTestMessage,
        String webhookPublicId,
        String webhookUrl,
        boolean hasWebhookSigningSecret,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
