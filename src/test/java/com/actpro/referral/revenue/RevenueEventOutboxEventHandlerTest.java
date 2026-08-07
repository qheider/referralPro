package com.actpro.referral.revenue;

import com.actpro.referral.company.Company;
import com.actpro.referral.outbox.OutboxEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class RevenueEventOutboxEventHandlerTest {

    @Mock
    private RevenueEventService revenueEventService;

    private RevenueEventOutboxEventHandler handler;

    @BeforeEach
    void setUp() {
        // findAndRegisterModules() picks up jackson-datatype-jsr310 (already on the classpath via
        // spring-boot-starter-json, same as the app's real ObjectMapper bean) so LocalDateTime
        // deserializes - a plain `new ObjectMapper()` doesn't support it out of the box.
        handler = new RevenueEventOutboxEventHandler(revenueEventService, new ObjectMapper().findAndRegisterModules());
    }

    @Test
    void shouldSupportOnlyReferralStatusChanged() {
        assertTrue(handler.supports("referral.status_changed"));
        assertFalse(handler.supports("referral.lead_registered"));
    }

    @Test
    void shouldParsePayloadAndDelegateToService() throws Exception {
        Company company = new Company();
        company.setId(5L);
        OutboxEvent event = new OutboxEvent();
        event.setCompany(company);
        event.setPayload("{\"referralId\":42,\"revenueAmount\":150.00,\"currency\":\"USD\",\"occurredAt\":\"2026-01-01T10:00:00\"}");

        handler.handle(event);

        verify(revenueEventService).applyReferralStatusChange(company, 42L, new BigDecimal("150.00"), "USD",
                java.time.LocalDateTime.of(2026, 1, 1, 10, 0, 0));
    }
}
