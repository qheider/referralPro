package com.actpro.referral.revenue;

import com.actpro.referral.integration.webhook.dto.ReferralStatusChangedEventPayload;
import com.actpro.referral.outbox.OutboxEvent;
import com.actpro.referral.outbox.OutboxEventHandler;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * Reacts to {@code referral.status_changed} outbox events (published by
 * {@code WebhookProcessingService}) by delegating to {@link RevenueEventService} - the Phase 8
 * consumer the {@code CompanyIntegration.rewardMappingJson} field was seeded for back in Phase 6.
 */
@Component
@Order(1)
@RequiredArgsConstructor
public class RevenueEventOutboxEventHandler implements OutboxEventHandler {

    private static final String SUPPORTED_EVENT_TYPE = "referral.status_changed";

    private final RevenueEventService revenueEventService;
    private final ObjectMapper objectMapper;

    @Override
    public boolean supports(String eventType) {
        return SUPPORTED_EVENT_TYPE.equals(eventType);
    }

    @Override
    public void handle(OutboxEvent event) throws Exception {
        ReferralStatusChangedEventPayload payload = objectMapper.readValue(event.getPayload(), ReferralStatusChangedEventPayload.class);
        revenueEventService.applyReferralStatusChange(
                event.getCompany(), payload.referralId(), payload.revenueAmount(), payload.currency(), payload.occurredAt());
    }
}
