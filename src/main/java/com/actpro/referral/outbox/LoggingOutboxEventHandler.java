package com.actpro.referral.outbox;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Default {@link OutboxEventHandler} for the phase this codebase is in: the outbox is
 * plumbing only, no business capability publishes real events through it yet and no downstream
 * consumer (e.g. Luup) is wired up. Logs and always succeeds so claimed rows still reach
 * PUBLISHED. Replace/override this bean once a real delivery mechanism lands.
 */
@Component
@Slf4j
public class LoggingOutboxEventHandler implements OutboxEventHandler {

    @Override
    public void handle(OutboxEvent event) {
        log.info(
                "Outbox event {} ({} on {}#{}) ready for delivery - no downstream consumer wired up yet",
                event.getId(), event.getEventType(), event.getAggregateType(), event.getAggregateId()
        );
    }
}
