package com.actpro.referral.integration;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

/**
 * Stage 2 of the two-stage outbound delivery pipeline: polling relay that claims a batch of due
 * {@link ApiSubmission} rows and hands each to {@link ApiSubmissionDispatchService} for HTTP
 * delivery, one short transaction per step - mirrors {@code outbox.OutboxDispatcher} exactly, and
 * for the same reason (a slow/failing company API can't block the request thread or the generic
 * outbox loop).
 */
@Component
@RequiredArgsConstructor
@Slf4j
@ConditionalOnProperty(name = "app.integration.dispatch-enabled", havingValue = "true", matchIfMissing = true)
public class ApiSubmissionDispatcher {

    private final ApiSubmissionDispatchService apiSubmissionDispatchService;

    @Value("${app.integration.batch-size:50}")
    private int batchSize;

    @Scheduled(fixedDelayString = "${app.integration.dispatch-interval-ms:5000}")
    public void dispatchPendingSubmissions() {
        String claimToken = UUID.randomUUID().toString();
        List<ApiSubmission> batch = apiSubmissionDispatchService.claimBatch(claimToken, batchSize);
        if (batch.isEmpty()) {
            return;
        }

        log.debug("Claimed {} api submission(s) for dispatch (token {})", batch.size(), claimToken);
        for (ApiSubmission submission : batch) {
            apiSubmissionDispatchService.dispatchOne(submission);
        }
    }
}
