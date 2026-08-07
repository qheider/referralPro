package com.actpro.referral.outbox;

/**
 * Delivers a claimed outbox event to whatever downstream consumer owns {@code eventType} (e.g.
 * the Phase 6 company-integration submission pipeline). Throw to signal delivery failure - the
 * dispatcher will retry with backoff up to a configured attempt limit, then dead-letter the row.
 * <p>
 * {@link OutboxDispatchService} injects every {@code OutboxEventHandler} bean and picks the first
 * whose {@link #supports(String)} matches a given event's type (Spring {@code @Order} controls
 * precedence among matches) - this lets multiple event-type-specific handlers coexist instead of
 * requiring exactly one bean for the whole app.
 */
public interface OutboxEventHandler {

    boolean supports(String eventType);

    void handle(OutboxEvent event) throws Exception;
}
