package com.actpro.referral.outbox;

import lombok.extern.slf4j.Slf4j;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * Catch-all {@link OutboxEventHandler}: matches any event type ({@link #supports} always returns
 * {@code true}) and is ordered last ({@link Ordered#LOWEST_PRECEDENCE}) so a more specific handler
 * (e.g. {@code CreateUserSubmissionOutboxEventHandler} for {@code referral.lead_registered})
 * always wins first. Logs and always succeeds so claimed rows still reach PUBLISHED - this is the
 * intended long-term role for any event type published without a real handler wired up yet, not
 * a placeholder to be deleted.
 */
@Component
@Order(Ordered.LOWEST_PRECEDENCE)
@Slf4j
public class LoggingOutboxEventHandler implements OutboxEventHandler {

    @Override
    public boolean supports(String eventType) {
        return true;
    }

    @Override
    public void handle(OutboxEvent event) {
        log.info(
                "Outbox event {} ({} on {}#{}) ready for delivery - no downstream consumer wired up yet",
                event.getId(), event.getEventType(), event.getAggregateType(), event.getAggregateId()
        );
    }
}
