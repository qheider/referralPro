package com.actpro.referral.integration;

import com.actpro.referral.outbox.OutboxEvent;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class CreateUserSubmissionOutboxEventHandlerTest {

    @Mock
    private ApiSubmissionService apiSubmissionService;

    @InjectMocks
    private CreateUserSubmissionOutboxEventHandler handler;

    @Test
    void shouldSupportOnlyLeadRegisteredEventType() {
        assertTrue(handler.supports("referral.lead_registered"));
        assertFalse(handler.supports("referral.converted"));
        assertFalse(handler.supports("some.other.event"));
    }

    @Test
    void shouldDelegateToApiSubmissionServiceWithoutMakingAnyHttpCall() {
        OutboxEvent event = new OutboxEvent();
        event.setEventType("referral.lead_registered");

        handler.handle(event);

        verify(apiSubmissionService).createOrFindSubmission(event);
    }
}
