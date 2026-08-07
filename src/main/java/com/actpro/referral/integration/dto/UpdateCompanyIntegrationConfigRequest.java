package com.actpro.referral.integration.dto;

import com.actpro.referral.integration.IntegrationAuthType;

/**
 * Credential fields are validated conditionally per {@code authType} in
 * {@code CompanyIntegrationService} (doesn't fit {@code @NotBlank} cleanly - required-ness
 * depends on another field's value). Omitting all credential fields for the current auth type on
 * an update keeps the existing stored credentials unchanged; supplying any of them replaces the
 * whole set for that type.
 */
public record UpdateCompanyIntegrationConfigRequest(
        String apiBaseUrl,
        IntegrationAuthType authType,
        String apiKeyHeaderName,
        String apiKeyValue,
        String bearerToken,
        String basicUsername,
        String basicPassword,
        Integer requestTimeoutMs,
        Integer maxRetryAttempts,
        String statusMappingJson,
        String rewardMappingJson
) {
}
