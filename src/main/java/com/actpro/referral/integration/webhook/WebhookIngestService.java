package com.actpro.referral.integration.webhook;

import com.actpro.referral.common.exception.BadRequestException;
import com.actpro.referral.common.exception.NotFoundException;
import com.actpro.referral.common.exception.UnauthorizedException;
import com.actpro.referral.company.CompanyIntegration;
import com.actpro.referral.company.CompanyIntegrationRepository;
import com.actpro.referral.integration.CredentialEncryptionService;
import com.actpro.referral.integration.webhook.dto.IncomingServiceStatusPayload;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * The synchronous half of webhook processing: resolve company, verify signature/timestamp, parse
 * just enough to durably store the event, and return - fast, one transaction, no referral
 * matching or status-mapping logic here. That's {@link WebhookProcessingService}'s job, run later
 * and asynchronously by {@link WebhookDispatcher}. Storage uses
 * {@link WebhookEventRepository#insertIgnoreDuplicate} (native INSERT IGNORE) rather than an
 * entity save()+catch - see that method's Javadoc for why a caught DataIntegrityViolationException
 * from a flush doesn't actually let the transaction commit cleanly afterward.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class WebhookIngestService {

    private final CompanyIntegrationRepository companyIntegrationRepository;
    private final CredentialEncryptionService credentialEncryptionService;
    private final WebhookSignatureVerifier webhookSignatureVerifier;
    private final WebhookEventRepository webhookEventRepository;
    private final ObjectMapper objectMapper;

    @Value("${app.webhook.timestamp-tolerance-seconds:300}")
    private int toleranceSeconds;

    @Value("${app.webhook.default-max-attempts:5}")
    private int defaultMaxAttempts;

    @Transactional
    public void ingest(String companyCode, String signature, String timestamp, String rawBody) {
        CompanyIntegration integration = companyIntegrationRepository.findByWebhookPublicId(companyCode)
                .orElseThrow(() -> new NotFoundException("Unknown webhook endpoint"));

        if (signature == null || signature.isBlank() || timestamp == null || timestamp.isBlank()) {
            throw new UnauthorizedException("Missing webhook signature headers");
        }
        if (integration.getWebhookSigningSecret() == null) {
            // Treated identically to "invalid signature" - don't leak whether a secret is configured.
            throw new UnauthorizedException("Invalid or stale webhook signature");
        }

        String decryptedSecret = credentialEncryptionService.decrypt(integration.getWebhookSigningSecret());
        if (!webhookSignatureVerifier.isValid(rawBody, signature, timestamp, decryptedSecret, toleranceSeconds)) {
            throw new UnauthorizedException("Invalid or stale webhook signature");
        }

        IncomingServiceStatusPayload payload = parse(rawBody);
        if (payload.eventId() == null || payload.eventId().isBlank()) {
            throw new BadRequestException("eventId is required");
        }

        // Relies on the DB unique constraint (company_id, event_id) to reject a concurrent/
        // redelivered duplicate, rather than a pre-check-then-insert, which races under
        // concurrent redelivery (companies commonly retry a webhook whose response they didn't
        // see in time).
        int inserted = webhookEventRepository.insertIgnoreDuplicate(
                integration.getCompany().getId(),
                payload.eventId(),
                payload.eventType() != null ? payload.eventType() : "UNKNOWN",
                rawBody,
                defaultMaxAttempts,
                LocalDateTime.now());
        if (inserted == 0) {
            log.info("Duplicate webhook event {} for company {} - acknowledged, not reprocessed",
                    payload.eventId(), integration.getCompany().getId());
        }
    }

    private IncomingServiceStatusPayload parse(String rawBody) {
        try {
            return objectMapper.readValue(rawBody, IncomingServiceStatusPayload.class);
        } catch (Exception e) {
            throw new BadRequestException("Malformed webhook payload");
        }
    }
}
