package com.actpro.referral.integration.webhook;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

/**
 * Polling relay: claims a batch of due {@link WebhookEvent} rows and hands each to
 * {@link WebhookProcessingService} for processing, one short transaction per step - mirrors
 * {@code outbox.OutboxDispatcher}/{@code integration.ApiSubmissionDispatcher} exactly.
 */
@Component
@RequiredArgsConstructor
@Slf4j
@ConditionalOnProperty(name = "app.webhook.dispatch-enabled", havingValue = "true", matchIfMissing = true)
public class WebhookDispatcher {

    private final WebhookProcessingService webhookProcessingService;

    @Value("${app.webhook.batch-size:50}")
    private int batchSize;

    @Scheduled(fixedDelayString = "${app.webhook.dispatch-interval-ms:5000}")
    public void dispatchPendingEvents() {
        String claimToken = UUID.randomUUID().toString();
        List<WebhookEvent> batch = webhookProcessingService.claimBatch(claimToken, batchSize);
        if (batch.isEmpty()) {
            return;
        }

        log.debug("Claimed {} webhook event(s) for processing (token {})", batch.size(), claimToken);
        for (WebhookEvent event : batch) {
            webhookProcessingService.processOne(event);
        }
    }
}
