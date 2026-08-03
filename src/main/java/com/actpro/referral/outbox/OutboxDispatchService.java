package com.actpro.referral.outbox;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Transactional halves of a dispatch cycle, kept separate from {@link OutboxDispatcher}'s
 * {@code @Scheduled} entry point so each gets its own short transaction: claiming releases its
 * row locks immediately, and one event's delivery/failure is committed independently of its
 * neighbors in the same batch. (A single method calling itself wouldn't get separate
 * transactions - Spring's @Transactional proxy doesn't apply to self-invocation - hence the
 * separate bean.)
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class OutboxDispatchService {

    private final OutboxEventRepository outboxEventRepository;
    private final OutboxEventHandler outboxEventHandler;

    @Value("${app.outbox.max-attempts:5}")
    private int maxAttempts;

    @Transactional
    public List<OutboxEvent> claimBatch(String claimToken, int batchSize) {
        int claimed = outboxEventRepository.claimBatch(claimToken, LocalDateTime.now(), batchSize);
        if (claimed == 0) {
            return List.of();
        }
        return outboxEventRepository.findByLockedBy(claimToken);
    }

    @Transactional
    public void dispatchOne(OutboxEvent event) {
        try {
            outboxEventHandler.handle(event);
            event.setStatus(OutboxEventStatus.PUBLISHED);
            event.setPublishedAt(LocalDateTime.now());
            event.setLockedBy(null);
        } catch (Exception ex) {
            applyFailure(event, ex);
        }
        outboxEventRepository.save(event);
    }

    private void applyFailure(OutboxEvent event, Exception ex) {
        int attempts = event.getAttempts() + 1;
        event.setAttempts(attempts);
        event.setLastError(truncate(ex.getMessage()));
        event.setLockedBy(null);

        if (attempts >= maxAttempts) {
            event.setStatus(OutboxEventStatus.DEAD_LETTER);
            log.error("Outbox event {} moved to DEAD_LETTER after {} attempts: {}",
                    event.getId(), attempts, ex.getMessage());
        } else {
            event.setStatus(OutboxEventStatus.PENDING);
            event.setAvailableAt(LocalDateTime.now().plusSeconds(backoffSeconds(attempts)));
            log.warn("Outbox event {} dispatch failed (attempt {}/{}), retrying at {}: {}",
                    event.getId(), attempts, maxAttempts, event.getAvailableAt(), ex.getMessage());
        }
    }

    private long backoffSeconds(int attempts) {
        return Math.min(300, (long) Math.pow(2, attempts));
    }

    private String truncate(String message) {
        if (message == null) {
            return null;
        }
        return message.length() > 2000 ? message.substring(0, 2000) : message;
    }
}
