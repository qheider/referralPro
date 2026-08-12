package com.actpro.referral.outbox;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

/**
 * Polling relay: claims a batch of due outbox rows and hands each to
 * {@link OutboxDispatchService} for delivery, one short transaction per step (see that class for
 * why). Runs on the scheduler thread, never the request thread, so a slow/failing downstream
 * consumer can't block API responses.
 */
@Component
@RequiredArgsConstructor
@Slf4j
@ConditionalOnProperty(name = "app.outbox.dispatch-enabled", havingValue = "true", matchIfMissing = true)
public class OutboxDispatcher {

    private final OutboxDispatchService outboxDispatchService;

    @Value("${app.outbox.batch-size:50}")
    private int batchSize;

    @Scheduled(fixedDelayString = "${app.outbox.dispatch-interval-ms:5000}")
    public void dispatchPendingEvents() {
        String claimToken = UUID.randomUUID().toString();
        List<OutboxEvent> batch = outboxDispatchService.claimBatch(claimToken, batchSize);
        if (batch.isEmpty()) {
            return;
        }

        log.debug("Claimed {} outbox event(s) for dispatch (token {})", batch.size(), claimToken);
        for (OutboxEvent event : batch) {
            outboxDispatchService.dispatchOne(event);
        }
    }
}
