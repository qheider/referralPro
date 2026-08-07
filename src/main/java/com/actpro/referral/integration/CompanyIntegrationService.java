package com.actpro.referral.integration;

import com.actpro.referral.common.exception.BadRequestException;
import com.actpro.referral.common.exception.NotFoundException;
import com.actpro.referral.company.CompanyIntegration;
import com.actpro.referral.company.CompanyIntegrationRepository;
import com.actpro.referral.company.CompanyIntegrationStatus;
import com.actpro.referral.integration.dto.ApiSubmissionDetailResponse;
import com.actpro.referral.integration.dto.ApiSubmissionSummaryResponse;
import com.actpro.referral.integration.dto.CompanyIntegrationConfigResponse;
import com.actpro.referral.integration.dto.IntegrationAttemptResponse;
import com.actpro.referral.integration.dto.TestConnectionResponse;
import com.actpro.referral.integration.dto.UpdateCompanyIntegrationConfigRequest;
import com.actpro.referral.integration.dto.CreateUserApiCallResult;
import com.actpro.referral.integration.webhook.ReferralStatusMappingService;
import com.actpro.referral.integration.webhook.WebhookEvent;
import com.actpro.referral.integration.webhook.WebhookEventRepository;
import com.actpro.referral.integration.webhook.WebhookEventStatus;
import com.actpro.referral.integration.webhook.dto.GenerateWebhookSecretResponse;
import com.actpro.referral.integration.webhook.dto.WebhookEventDetailResponse;
import com.actpro.referral.integration.webhook.dto.WebhookEventSummaryResponse;
import com.actpro.referral.security.CurrentUserService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Limit;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Company-scoped config CRUD, Test Connection, enable/disable lifecycle, and read-only submission
 * monitoring - mirrors {@code CompanyApiKeyService}'s style: no {@code {companyId}} path variable,
 * every method resolves the caller's company via {@link CurrentUserService#getCurrentCompanyId()}
 * (there's exactly one {@link CompanyIntegration} row per company).
 */
@Service
@RequiredArgsConstructor
public class CompanyIntegrationService {

    private static final int MAX_MESSAGE_LENGTH = 2000;
    private static final String WEBHOOK_SECRET_ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz23456789";
    private static final int WEBHOOK_SECRET_LENGTH = 40;
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final CompanyIntegrationRepository companyIntegrationRepository;
    private final ApiSubmissionRepository apiSubmissionRepository;
    private final IntegrationAttemptRepository integrationAttemptRepository;
    private final WebhookEventRepository webhookEventRepository;
    private final CredentialEncryptionService credentialEncryptionService;
    private final CreateUserApiClient createUserApiClient;
    private final ReferralStatusMappingService referralStatusMappingService;
    private final CurrentUserService currentUserService;
    private final ObjectMapper objectMapper;

    @Value("${app.base-url}")
    private String baseUrl;

    @Transactional(readOnly = true)
    public CompanyIntegrationConfigResponse getConfig() {
        return toConfigResponse(getCurrentIntegration());
    }

    @Transactional
    public CompanyIntegrationConfigResponse updateConfig(UpdateCompanyIntegrationConfigRequest request) {
        CompanyIntegration integration = getCurrentIntegration();

        if (request.apiBaseUrl() == null || request.apiBaseUrl().isBlank()) {
            throw new BadRequestException("API base URL is required");
        }
        IntegrationAuthType authType = request.authType() != null ? request.authType() : IntegrationAuthType.NONE;

        CredentialResolution credentials = resolveCredentials(integration, authType, request);
        if (credentials.changed()) {
            integration.setEncryptedCredentials(credentials.encryptedValue());
        }

        integration.setApiBaseUrl(request.apiBaseUrl().trim());
        integration.setAuthType(authType);

        if (request.requestTimeoutMs() != null) {
            if (request.requestTimeoutMs() <= 0) {
                throw new BadRequestException("Request timeout must be positive");
            }
            integration.setRequestTimeoutMs(request.requestTimeoutMs());
        }
        if (request.maxRetryAttempts() != null) {
            if (request.maxRetryAttempts() < 0) {
                throw new BadRequestException("Max retry attempts cannot be negative");
            }
            integration.setMaxRetryAttempts(request.maxRetryAttempts());
        }

        String statusMappingJson = validateJsonOrNull(request.statusMappingJson(), "statusMappingJson");
        referralStatusMappingService.findFirstInvalidMappingValue(statusMappingJson).ifPresent(invalidValue -> {
            throw new BadRequestException("statusMappingJson value '" + invalidValue + "' is not a valid, mappable ReferralStatus");
        });
        integration.setStatusMappingJson(statusMappingJson);
        integration.setRewardMappingJson(validateJsonOrNull(request.rewardMappingJson(), "rewardMappingJson"));

        if (integration.getStatus() == CompanyIntegrationStatus.NOT_CONFIGURED
                || integration.getStatus() == CompanyIntegrationStatus.ERROR) {
            integration.setStatus(CompanyIntegrationStatus.PENDING_VERIFICATION);
        }

        return toConfigResponse(companyIntegrationRepository.save(integration));
    }

    /**
     * Best-effort connectivity+auth probe - there's no known real health-check contract for an
     * arbitrary company's Create User endpoint, so "success" is defined as any HTTP response
     * other than 401/403. A connection/timeout failure leaves the integration's status untouched
     * (still PENDING_VERIFICATION, say) rather than downgrading it to ERROR, since that's a
     * reachability problem, not necessarily a credentials problem.
     */
    @Transactional
    public TestConnectionResponse testConnection() {
        CompanyIntegration integration = getCurrentIntegration();
        if (integration.getApiBaseUrl() == null || integration.getApiBaseUrl().isBlank()) {
            throw new BadRequestException("Configure an API base URL before testing the connection");
        }

        CreateUserApiCallResult result = createUserApiClient.testConnection(integration);
        LocalDateTime testedAt = LocalDateTime.now();
        boolean authFailure = result.ioSuccess() && result.httpStatus() != null
                && (result.httpStatus() == 401 || result.httpStatus() == 403);
        boolean success = result.ioSuccess() && !authFailure;

        String message = !result.ioSuccess()
                ? result.sanitizedErrorMessage()
                : authFailure
                ? "Authentication failed (HTTP " + result.httpStatus() + ")"
                : "Reached the configured endpoint (HTTP " + result.httpStatus() + ")";

        integration.setLastTestedAt(testedAt);
        integration.setLastTestResult(success ? "SUCCESS" : "FAILURE");
        integration.setLastTestMessage(truncate(message));

        if (success) {
            integration.setStatus(CompanyIntegrationStatus.ACTIVE);
        } else if (authFailure) {
            integration.setStatus(CompanyIntegrationStatus.ERROR);
        }
        companyIntegrationRepository.save(integration);

        return new TestConnectionResponse(
                success, result.ioSuccess() ? result.httpStatus() : null, message, testedAt, integration.getStatus());
    }

    @Transactional
    public CompanyIntegrationConfigResponse enable() {
        CompanyIntegration integration = getCurrentIntegration();
        if (integration.getStatus() != CompanyIntegrationStatus.DISABLED) {
            throw new BadRequestException("Only a DISABLED integration can be enabled");
        }
        integration.setStatus(CompanyIntegrationStatus.ACTIVE);
        return toConfigResponse(companyIntegrationRepository.save(integration));
    }

    /**
     * Doesn't cancel in-flight ApiSubmission rows - they simply stop being claimed (the dispatch
     * claim query only pulls submissions for ACTIVE integrations) and resume automatically if
     * re-enabled.
     */
    @Transactional
    public CompanyIntegrationConfigResponse disable() {
        CompanyIntegration integration = getCurrentIntegration();
        if (integration.getStatus() == CompanyIntegrationStatus.DISABLED
                || integration.getStatus() == CompanyIntegrationStatus.NOT_CONFIGURED) {
            throw new BadRequestException("Integration is already disabled or not configured");
        }
        integration.setStatus(CompanyIntegrationStatus.DISABLED);
        return toConfigResponse(companyIntegrationRepository.save(integration));
    }

    /** Raw value returned once; only the encrypted form is ever persisted or re-exposed. */
    @Transactional
    public GenerateWebhookSecretResponse generateWebhookSecret() {
        CompanyIntegration integration = getCurrentIntegration();
        String rawSecret = randomString(WEBHOOK_SECRET_ALPHABET, WEBHOOK_SECRET_LENGTH);
        integration.setWebhookSigningSecret(credentialEncryptionService.encrypt(rawSecret));
        LocalDateTime generatedAt = LocalDateTime.now();
        companyIntegrationRepository.save(integration);
        return new GenerateWebhookSecretResponse(rawSecret, generatedAt);
    }

    @Transactional(readOnly = true)
    public List<WebhookEventSummaryResponse> listWebhookEvents(WebhookEventStatus status, int limit) {
        Long companyId = currentUserService.getCurrentCompanyId();
        List<WebhookEvent> events = status != null
                ? webhookEventRepository.findByCompanyIdAndStatusOrderByCreatedAtDesc(companyId, status, Limit.of(limit))
                : webhookEventRepository.findByCompanyIdOrderByCreatedAtDesc(companyId, Limit.of(limit));
        return events.stream().map(this::toWebhookSummary).toList();
    }

    @Transactional(readOnly = true)
    public WebhookEventDetailResponse getWebhookEventDetail(Long webhookEventId) {
        Long companyId = currentUserService.getCurrentCompanyId();
        WebhookEvent event = webhookEventRepository.findByIdAndCompanyId(webhookEventId, companyId)
                .orElseThrow(() -> new NotFoundException("Webhook event not found"));
        return new WebhookEventDetailResponse(
                event.getId(),
                event.getEventId(),
                event.getEventType(),
                event.getStatus(),
                event.getRawPayload(),
                event.getMatchedReferralId(),
                event.getMappedStatus(),
                event.getFailureReason(),
                event.getAttempts(),
                event.getMaxAttempts(),
                event.getAvailableAt(),
                event.getCreatedAt(),
                event.getProcessedAt()
        );
    }

    @Transactional(readOnly = true)
    public List<ApiSubmissionSummaryResponse> listSubmissions(ApiSubmissionStatus status, int limit) {
        Long companyId = currentUserService.getCurrentCompanyId();
        List<ApiSubmission> submissions = status != null
                ? apiSubmissionRepository.findByCompanyIdAndStatusOrderByCreatedAtDesc(companyId, status, Limit.of(limit))
                : apiSubmissionRepository.findByCompanyIdOrderByCreatedAtDesc(companyId, Limit.of(limit));
        return submissions.stream().map(this::toSummary).toList();
    }

    @Transactional(readOnly = true)
    public ApiSubmissionDetailResponse getSubmissionDetail(Long submissionId) {
        Long companyId = currentUserService.getCurrentCompanyId();
        ApiSubmission submission = apiSubmissionRepository.findByIdAndCompanyId(submissionId, companyId)
                .orElseThrow(() -> new NotFoundException("Submission not found"));
        List<IntegrationAttemptResponse> attempts = integrationAttemptRepository
                .findByApiSubmissionIdOrderByAttemptNumberAsc(submissionId).stream()
                .map(this::toAttemptResponse)
                .toList();

        return new ApiSubmissionDetailResponse(
                submission.getId(),
                submission.getExternalRequestId(),
                submission.getStatus(),
                submission.getAttempts(),
                submission.getMaxAttempts(),
                submission.getAggregateId(),
                submission.getLastError(),
                submission.getCompanyCustomerReference(),
                submission.getCompanyTransactionReference(),
                submission.getAvailableAt(),
                submission.getSubmittedAt(),
                submission.getCreatedAt(),
                attempts
        );
    }

    private CompanyIntegration getCurrentIntegration() {
        Long companyId = currentUserService.getCurrentCompanyId();
        return companyIntegrationRepository.findByCompanyId(companyId)
                .orElseThrow(() -> new NotFoundException("Company integration not found"));
    }

    private CredentialResolution resolveCredentials(
            CompanyIntegration integration, IntegrationAuthType authType, UpdateCompanyIntegrationConfigRequest request) {
        if (authType == IntegrationAuthType.NONE) {
            return new CredentialResolution(true, null);
        }

        boolean anySupplied = switch (authType) {
            case API_KEY -> !isBlank(request.apiKeyHeaderName()) || !isBlank(request.apiKeyValue());
            case BEARER_TOKEN -> !isBlank(request.bearerToken());
            case BASIC -> !isBlank(request.basicUsername()) || !isBlank(request.basicPassword());
            case NONE -> false;
        };

        if (!anySupplied) {
            boolean canKeepExisting = integration.getAuthType() == authType && integration.getEncryptedCredentials() != null;
            if (canKeepExisting) {
                return new CredentialResolution(false, null);
            }
            throw new BadRequestException("Credentials are required for auth type " + authType);
        }

        Map<String, String> fields = new LinkedHashMap<>();
        switch (authType) {
            case API_KEY -> {
                requireNonBlank(request.apiKeyHeaderName(), "apiKeyHeaderName");
                requireNonBlank(request.apiKeyValue(), "apiKeyValue");
                fields.put("headerName", request.apiKeyHeaderName());
                fields.put("headerValue", request.apiKeyValue());
            }
            case BEARER_TOKEN -> {
                requireNonBlank(request.bearerToken(), "bearerToken");
                fields.put("token", request.bearerToken());
            }
            case BASIC -> {
                requireNonBlank(request.basicUsername(), "basicUsername");
                requireNonBlank(request.basicPassword(), "basicPassword");
                fields.put("username", request.basicUsername());
                fields.put("password", request.basicPassword());
            }
            case NONE -> { /* unreachable, guarded above */ }
        }

        return new CredentialResolution(true, credentialEncryptionService.encrypt(toJson(fields)));
    }

    private String validateJsonOrNull(String json, String fieldName) {
        if (json == null || json.isBlank()) {
            return null;
        }
        try {
            objectMapper.readTree(json);
        } catch (JsonProcessingException e) {
            throw new BadRequestException(fieldName + " must be valid JSON");
        }
        return json;
    }

    private String toJson(Map<String, String> fields) {
        try {
            return objectMapper.writeValueAsString(fields);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to serialize integration credentials", e);
        }
    }

    private void requireNonBlank(String value, String fieldName) {
        if (isBlank(value)) {
            throw new BadRequestException(fieldName + " is required");
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private String randomString(String alphabet, int length) {
        StringBuilder builder = new StringBuilder(length);
        for (int index = 0; index < length; index++) {
            builder.append(alphabet.charAt(SECURE_RANDOM.nextInt(alphabet.length())));
        }
        return builder.toString();
    }

    private String truncate(String message) {
        if (message == null) {
            return null;
        }
        return message.length() > MAX_MESSAGE_LENGTH ? message.substring(0, MAX_MESSAGE_LENGTH) : message;
    }

    private CompanyIntegrationConfigResponse toConfigResponse(CompanyIntegration integration) {
        return new CompanyIntegrationConfigResponse(
                integration.getId(),
                integration.getStatus(),
                integration.getApiBaseUrl(),
                integration.getAuthType(),
                integration.getEncryptedCredentials() != null,
                integration.getRequestTimeoutMs(),
                integration.getMaxRetryAttempts(),
                integration.getStatusMappingJson(),
                integration.getRewardMappingJson(),
                integration.getLastTestedAt(),
                integration.getLastTestResult(),
                integration.getLastTestMessage(),
                integration.getWebhookPublicId(),
                webhookUrl(integration.getWebhookPublicId()),
                integration.getWebhookSigningSecret() != null,
                integration.getCreatedAt(),
                integration.getUpdatedAt()
        );
    }

    private String webhookUrl(String webhookPublicId) {
        return baseUrl + "/api/v1/integrations/" + webhookPublicId + "/webhooks/service-status";
    }

    private WebhookEventSummaryResponse toWebhookSummary(WebhookEvent event) {
        return new WebhookEventSummaryResponse(
                event.getId(),
                event.getEventId(),
                event.getEventType(),
                event.getStatus(),
                event.getMatchedReferralId(),
                event.getMappedStatus(),
                event.getFailureReason(),
                event.getCreatedAt(),
                event.getProcessedAt()
        );
    }

    private ApiSubmissionSummaryResponse toSummary(ApiSubmission submission) {
        return new ApiSubmissionSummaryResponse(
                submission.getId(),
                submission.getExternalRequestId(),
                submission.getStatus(),
                submission.getAttempts(),
                submission.getMaxAttempts(),
                submission.getAggregateId(),
                submission.getLastError(),
                submission.getAvailableAt(),
                submission.getSubmittedAt(),
                submission.getCreatedAt()
        );
    }

    private IntegrationAttemptResponse toAttemptResponse(IntegrationAttempt attempt) {
        return new IntegrationAttemptResponse(
                attempt.getAttemptNumber(),
                attempt.getStartedAt(),
                attempt.getCompletedAt(),
                attempt.getHttpStatus(),
                attempt.getOutcome(),
                attempt.getFailureCategory(),
                attempt.getSanitizedMessage(),
                attempt.getNextRetryAt()
        );
    }

    private record CredentialResolution(boolean changed, String encryptedValue) {
    }
}
