package com.actpro.referral.integration.webhook;

import com.actpro.referral.company.CompanyIntegration;
import com.actpro.referral.company.CompanyIntegrationRepository;
import com.actpro.referral.integration.ApiSubmission;
import com.actpro.referral.integration.ApiSubmissionRepository;
import com.actpro.referral.integration.webhook.dto.IncomingServiceStatusPayload;
import com.actpro.referral.referral.Referral;
import com.actpro.referral.referral.ReferralRepository;
import com.actpro.referral.referral.ReferralStatus;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Async half of webhook processing: claim/dispatch transactional halves, mirroring
 * {@code integration.ApiSubmissionDispatchService}'s two-method split (claiming releases its row
 * locks immediately; one event's processing/failure commits independently of its neighbors).
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class WebhookProcessingService {

    private static final int MAX_BACKOFF_SECONDS = 300;

    private final WebhookEventRepository webhookEventRepository;
    private final CompanyIntegrationRepository companyIntegrationRepository;
    private final ApiSubmissionRepository apiSubmissionRepository;
    private final ReferralRepository referralRepository;
    private final ReferralStatusMappingService referralStatusMappingService;
    private final ObjectMapper objectMapper;

    @Transactional
    public List<WebhookEvent> claimBatch(String claimToken, int batchSize) {
        int claimed = webhookEventRepository.claimBatch(claimToken, LocalDateTime.now(), batchSize);
        if (claimed == 0) {
            return List.of();
        }
        return webhookEventRepository.findByLockedBy(claimToken);
    }

    @Transactional
    public void processOne(WebhookEvent event) {
        try {
            doProcess(event);
        } catch (Exception ex) {
            applyTransientFailure(event, ex);
        }
        event.setLockedBy(null);
        webhookEventRepository.save(event);
    }

    private void doProcess(WebhookEvent event) {
        IncomingServiceStatusPayload payload = parse(event.getRawPayload());

        Long companyId = event.getCompany().getId();
        Optional<ApiSubmission> matched = matchSubmission(companyId, payload);
        if (matched.isEmpty()) {
            markManualReview(event, "No matching referral for serviceReference/companyUserReference");
            return;
        }

        Optional<Referral> referralOpt = referralRepository.findById(matched.get().getAggregateId());
        if (referralOpt.isEmpty()) {
            markManualReview(event, "Matched ApiSubmission's referral no longer exists");
            return;
        }
        Referral referral = referralOpt.get();

        CompanyIntegration integration = companyIntegrationRepository.findByCompanyId(companyId).orElse(null);
        String statusMappingJson = integration != null ? integration.getStatusMappingJson() : null;
        Optional<ReferralStatus> mapped = referralStatusMappingService.mapStatus(statusMappingJson, payload.status());
        if (mapped.isEmpty()) {
            markManualReview(event, "No status mapping configured for status '" + payload.status() + "'");
            return;
        }

        event.setMatchedReferralId(referral.getId());
        event.setMappedStatus(mapped.get().name());

        if (!referralStatusMappingService.isTransitionAllowed(referral.getStatus(), mapped.get())) {
            event.setStatus(WebhookEventStatus.IGNORED);
            event.setProcessedAt(LocalDateTime.now());
            log.info("Webhook event {} ignored: {} -> {} not an allowed transition for referral {}",
                    event.getId(), referral.getStatus(), mapped.get(), referral.getId());
            return;
        }

        referral.setStatus(mapped.get());
        referralRepository.save(referral);

        event.setStatus(WebhookEventStatus.PROCESSED);
        event.setProcessedAt(LocalDateTime.now());
    }

    private Optional<ApiSubmission> matchSubmission(Long companyId, IncomingServiceStatusPayload payload) {
        if (payload.serviceReference() != null) {
            Optional<ApiSubmission> byTransactionRef =
                    apiSubmissionRepository.findByCompanyIdAndCompanyTransactionReference(companyId, payload.serviceReference());
            if (byTransactionRef.isPresent()) {
                return byTransactionRef;
            }
        }
        if (payload.companyUserReference() != null) {
            return apiSubmissionRepository.findByCompanyIdAndCompanyCustomerReference(companyId, payload.companyUserReference());
        }
        return Optional.empty();
    }

    private void markManualReview(WebhookEvent event, String reason) {
        event.setStatus(WebhookEventStatus.MANUAL_REVIEW);
        event.setFailureReason(reason);
        event.setProcessedAt(LocalDateTime.now());
        log.warn("Webhook event {} routed to MANUAL_REVIEW: {}", event.getId(), reason);
    }

    private void applyTransientFailure(WebhookEvent event, Exception ex) {
        int attempts = event.getAttempts() + 1;
        event.setAttempts(attempts);
        event.setFailureReason(truncate(ex.getMessage()));

        if (attempts >= event.getMaxAttempts()) {
            event.setStatus(WebhookEventStatus.MANUAL_REVIEW);
            event.setProcessedAt(LocalDateTime.now());
            log.error("Webhook event {} moved to MANUAL_REVIEW after {} attempts: {}", event.getId(), attempts, ex.getMessage());
        } else {
            event.setStatus(WebhookEventStatus.RETRY_SCHEDULED);
            event.setAvailableAt(LocalDateTime.now().plusSeconds(backoffSecondsWithJitter(attempts)));
            log.warn("Webhook event {} processing failed (attempt {}/{}), retrying at {}: {}",
                    event.getId(), attempts, event.getMaxAttempts(), event.getAvailableAt(), ex.getMessage());
        }
    }

    private long backoffSecondsWithJitter(int attempts) {
        double capped = Math.min(MAX_BACKOFF_SECONDS, Math.pow(2, attempts));
        double jitterFactor = 0.5 + ThreadLocalRandom.current().nextDouble() * 0.5;
        return Math.max(1, Math.round(capped * jitterFactor));
    }

    private IncomingServiceStatusPayload parse(String rawPayload) {
        try {
            return objectMapper.readValue(rawPayload, IncomingServiceStatusPayload.class);
        } catch (Exception e) {
            throw new IllegalStateException("Stored webhook payload is not valid JSON", e);
        }
    }

    private String truncate(String message) {
        if (message == null) {
            return null;
        }
        return message.length() > 2000 ? message.substring(0, 2000) : message;
    }
}
