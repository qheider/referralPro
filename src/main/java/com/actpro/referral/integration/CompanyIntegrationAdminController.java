package com.actpro.referral.integration;

import com.actpro.referral.common.ApiResponse;
import com.actpro.referral.integration.dto.ApiSubmissionDetailResponse;
import com.actpro.referral.integration.dto.ApiSubmissionSummaryResponse;
import com.actpro.referral.integration.dto.CompanyIntegrationConfigResponse;
import com.actpro.referral.integration.dto.TestConnectionResponse;
import com.actpro.referral.integration.dto.UpdateCompanyIntegrationConfigRequest;
import com.actpro.referral.integration.webhook.WebhookEventStatus;
import com.actpro.referral.integration.webhook.dto.GenerateWebhookSecretResponse;
import com.actpro.referral.integration.webhook.dto.WebhookEventDetailResponse;
import com.actpro.referral.integration.webhook.dto.WebhookEventSummaryResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Admin config, Test Connection, enable/disable, and read-only submission monitoring for the
 * company's outgoing Create User API integration - exactly one {@link
 * com.actpro.referral.company.CompanyIntegration} row per company, so (like {@link
 * com.actpro.referral.company.ApiKeyAdminController}) there's no {@code {companyId}} path
 * variable; every endpoint resolves the caller's company internally.
 */
@Tag(name = "Company Integration", description = "Outgoing Create User API configuration, testing, and delivery monitoring")
@RestController
@RequestMapping("/api/admin/company-integration")
@RequiredArgsConstructor
@PreAuthorize("hasRole('COMPANY_ADMIN')")
public class CompanyIntegrationAdminController {

    private final CompanyIntegrationService companyIntegrationService;

    @Operation(summary = "Get this company's integration config", description = "Never returns raw credential values - only hasCredentials")
    @GetMapping
    public ResponseEntity<ApiResponse<CompanyIntegrationConfigResponse>> getConfig() {
        return ResponseEntity.ok(ApiResponse.success(companyIntegrationService.getConfig()));
    }

    @Operation(summary = "Create or update the integration config")
    @PutMapping
    public ResponseEntity<ApiResponse<CompanyIntegrationConfigResponse>> updateConfig(
            @RequestBody UpdateCompanyIntegrationConfigRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Integration configuration saved", companyIntegrationService.updateConfig(request)));
    }

    @Operation(summary = "Test the configured connection", description = "Best-effort reachability+auth probe against the configured API URL")
    @PostMapping("/test-connection")
    public ResponseEntity<ApiResponse<TestConnectionResponse>> testConnection() {
        return ResponseEntity.ok(ApiResponse.success(companyIntegrationService.testConnection()));
    }

    @Operation(summary = "Re-enable a disabled integration")
    @PostMapping("/enable")
    public ResponseEntity<ApiResponse<CompanyIntegrationConfigResponse>> enable() {
        return ResponseEntity.ok(ApiResponse.success("Integration enabled", companyIntegrationService.enable()));
    }

    @Operation(summary = "Disable the integration", description = "Queued submissions are paused, not cancelled - they resume automatically if re-enabled")
    @PostMapping("/disable")
    public ResponseEntity<ApiResponse<CompanyIntegrationConfigResponse>> disable() {
        return ResponseEntity.ok(ApiResponse.success("Integration disabled", companyIntegrationService.disable()));
    }

    @Operation(summary = "List recent outgoing Create User submissions", description = "Optionally filtered by status")
    @GetMapping("/submissions")
    public ResponseEntity<ApiResponse<List<ApiSubmissionSummaryResponse>>> listSubmissions(
            @RequestParam(required = false) ApiSubmissionStatus status,
            @RequestParam(required = false, defaultValue = "50") int limit) {
        return ResponseEntity.ok(ApiResponse.success(companyIntegrationService.listSubmissions(status, limit)));
    }

    @Operation(summary = "Get a submission's full attempt history")
    @GetMapping("/submissions/{submissionId}")
    public ResponseEntity<ApiResponse<ApiSubmissionDetailResponse>> getSubmissionDetail(@PathVariable Long submissionId) {
        return ResponseEntity.ok(ApiResponse.success(companyIntegrationService.getSubmissionDetail(submissionId)));
    }

    @Operation(summary = "Generate/rotate the webhook signing secret", description = "The raw value is shown once, in this response only")
    @PostMapping("/webhook-secret")
    public ResponseEntity<ApiResponse<GenerateWebhookSecretResponse>> generateWebhookSecret() {
        return ResponseEntity.ok(ApiResponse.success("Webhook signing secret generated", companyIntegrationService.generateWebhookSecret()));
    }

    @Operation(summary = "List recent inbound webhook events", description = "Optionally filtered by status")
    @GetMapping("/webhook-events")
    public ResponseEntity<ApiResponse<List<WebhookEventSummaryResponse>>> listWebhookEvents(
            @RequestParam(required = false) WebhookEventStatus status,
            @RequestParam(required = false, defaultValue = "50") int limit) {
        return ResponseEntity.ok(ApiResponse.success(companyIntegrationService.listWebhookEvents(status, limit)));
    }

    @Operation(summary = "Get a webhook event's full detail", description = "Payload, match/mapping outcome, failure reason")
    @GetMapping("/webhook-events/{webhookEventId}")
    public ResponseEntity<ApiResponse<WebhookEventDetailResponse>> getWebhookEventDetail(@PathVariable Long webhookEventId) {
        return ResponseEntity.ok(ApiResponse.success(companyIntegrationService.getWebhookEventDetail(webhookEventId)));
    }
}
