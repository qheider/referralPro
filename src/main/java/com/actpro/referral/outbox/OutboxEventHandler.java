package com.actpro.referral.outbox;

/**
 * Delivers a claimed outbox event to whatever downstream consumer owns {@code eventType} (e.g.
 * the future Luup webhook/broker integration). Throw to signal delivery failure - the dispatcher
 * will retry with backoff up to a configured attempt limit, then dead-letter the row.
 */
public interface OutboxEventHandler {

    void handle(OutboxEvent event) throws Exception;
}
