package com.actpro.referral.integration;

import com.actpro.referral.outbox.OutboxEvent;
import com.actpro.referral.outbox.OutboxEventHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * Stage 1 of the two-stage outbound delivery pipeline (see {@link ApiSubmission}'s Javadoc):
 * reacts to {@code referral.lead_registered} outbox events by creating the durable
 * {@link ApiSubmission} row - deliberately performs no HTTP call itself, so the outbox event
 * reaches PUBLISHED as soon as that row exists. Actual delivery is
 * {@link ApiSubmissionDispatcher}'s job, on its own independent schedule/retry lifecycle.
 */
@Component
@Order(1)
@RequiredArgsConstructor
public class CreateUserSubmissionOutboxEventHandler implements OutboxEventHandler {

    private static final String SUPPORTED_EVENT_TYPE = "referral.lead_registered";

    private final ApiSubmissionService apiSubmissionService;

    @Override
    public boolean supports(String eventType) {
        return SUPPORTED_EVENT_TYPE.equals(eventType);
    }

    @Override
    public void handle(OutboxEvent event) {
        apiSubmissionService.createOrFindSubmission(event);
    }
}
