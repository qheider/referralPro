package com.actpro.referral.company;

import com.actpro.referral.common.BaseEntity;
import com.actpro.referral.integration.IntegrationAuthType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * Seeded (status NOT_CONFIGURED) at company registration time (see CompanyService.registerCompany)
 * then extended in place by Phase 6 (V29) with the outgoing Create User API config surface: base
 * URL, auth type, encrypted credentials, timeout, retry policy, and last test result, and by
 * Phase 7 (V30) with webhookPublicId (the non-guessable public identifier inbound webhooks
 * resolve this company by) and real use of webhookSigningSecret/statusMappingJson (encrypted
 * signing secret verified by {@link com.actpro.referral.integration.webhook.WebhookSignatureVerifier};
 * status mapping applied by {@link com.actpro.referral.integration.webhook.ReferralStatusMappingService}).
 * rewardMappingJson remains inert until Phase 8 - stored now so that phase doesn't need another
 * schema reshape. Credentials/secrets are never returned decrypted outside
 * {@link com.actpro.referral.integration.ApiSubmissionDispatchService},
 * {@link com.actpro.referral.integration.CompanyIntegrationService#testConnection()}, and
 * {@link com.actpro.referral.integration.webhook.WebhookIngestService}.
 */
@Entity
@Table(
        name = "company_integrations",
        uniqueConstraints = @UniqueConstraint(name = "uk_company_integrations_company_id", columnNames = "company_id")
)
@Getter
@Setter
@NoArgsConstructor
public class CompanyIntegration extends BaseEntity {

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "company_id", nullable = false)
    private Company company;

    // Non-guessable public identifier used to resolve this company from an inbound webhook URL
    // (/api/v1/integrations/{webhookPublicId}/webhooks/service-status) - never company.id or
    // campaign_code, so a leaked webhook URL doesn't expose anything used elsewhere.
    @Column(name = "webhook_public_id", nullable = false, length = 20)
    private String webhookPublicId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private CompanyIntegrationStatus status = CompanyIntegrationStatus.NOT_CONFIGURED;

    @Column(name = "api_base_url", length = 500)
    private String apiBaseUrl;

    @Enumerated(EnumType.STRING)
    @Column(name = "auth_type", nullable = false, length = 30)
    private IntegrationAuthType authType = IntegrationAuthType.NONE;

    // AES-GCM encrypted JSON blob, shape depends on authType - see CredentialEncryptionService.
    @Column(name = "encrypted_credentials", columnDefinition = "TEXT")
    private String encryptedCredentials;

    @Column(name = "request_timeout_ms", nullable = false)
    private int requestTimeoutMs = 10000;

    @Column(name = "max_retry_attempts", nullable = false)
    private int maxRetryAttempts = 5;

    @Column(name = "last_tested_at")
    private LocalDateTime lastTestedAt;

    @Column(name = "last_test_result", length = 20)
    private String lastTestResult;

    @Column(name = "last_test_message", length = 2000)
    private String lastTestMessage;

    // AES-GCM encrypted like the outbound credentials above. Generated via
    // CompanyIntegrationService#generateWebhookSecret, decrypted only by WebhookIngestService to
    // verify an inbound signature.
    @Column(name = "webhook_signing_secret", columnDefinition = "TEXT")
    private String webhookSigningSecret;

    // JSON object of company-status-string -> ReferralStatus name, e.g. {"SERVICE_COMPLETED":
    // "COMPLETED"}. Validated (syntax + each value is a real, non-EXPIRED ReferralStatus constant)
    // by CompanyIntegrationService#updateConfig; applied by ReferralStatusMappingService.
    @Column(name = "status_mapping_json", columnDefinition = "TEXT")
    private String statusMappingJson;

    // Opaque JSON, syntax-validated only. Unused until Phase 8.
    @Column(name = "reward_mapping_json", columnDefinition = "TEXT")
    private String rewardMappingJson;
}
