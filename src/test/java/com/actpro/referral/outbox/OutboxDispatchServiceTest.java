package com.actpro.referral.outbox;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OutboxDispatchServiceTest {

    @Mock
    private OutboxEventRepository outboxEventRepository;

    @Mock
    private OutboxEventHandler outboxEventHandler;

    private OutboxDispatchService outboxDispatchService;

    @BeforeEach
    void setUp() {
        // OutboxDispatchService now takes List<OutboxEventHandler> (routes to the first
        // supports()-matching handler) rather than a single injected bean, so it's constructed
        // manually here instead of via @InjectMocks.
        outboxDispatchService = new OutboxDispatchService(outboxEventRepository, List.of(outboxEventHandler));
        ReflectionTestUtils.setField(outboxDispatchService, "maxAttempts", 3);
    }

    @Test
    void shouldReturnEmptyBatchWithoutQueryingWhenNothingClaimed() {
        when(outboxEventRepository.claimBatch(eq("token-1"), any(LocalDateTime.class), eq(50))).thenReturn(0);

        List<OutboxEvent> batch = outboxDispatchService.claimBatch("token-1", 50);

        assertTrue(batch.isEmpty());
        verify(outboxEventRepository, never()).findByLockedBy(any());
    }

    @Test
    void shouldReturnClaimedRowsByLockToken() {
        OutboxEvent claimed = new OutboxEvent();
        claimed.setId(9L);
        when(outboxEventRepository.claimBatch(eq("token-2"), any(LocalDateTime.class), eq(50))).thenReturn(2);
        when(outboxEventRepository.findByLockedBy("token-2")).thenReturn(List.of(claimed));

        List<OutboxEvent> batch = outboxDispatchService.claimBatch("token-2", 50);

        assertEquals(1, batch.size());
        assertEquals(9L, batch.get(0).getId());
    }

    @Test
    void shouldMarkEventPublishedOnSuccessfulHandling() throws Exception {
        OutboxEvent event = claimedEvent();
        when(outboxEventHandler.supports(event.getEventType())).thenReturn(true);

        outboxDispatchService.dispatchOne(event);

        verify(outboxEventHandler).handle(event);
        assertEquals(OutboxEventStatus.PUBLISHED, event.getStatus());
        assertNotNull(event.getPublishedAt());
        assertNull(event.getLockedBy());
        verify(outboxEventRepository).save(event);
    }

    @Test
    void shouldRescheduleWithBackoffOnFailureBelowMaxAttempts() throws Exception {
        OutboxEvent event = claimedEvent();
        event.setAttempts(0);
        when(outboxEventHandler.supports(event.getEventType())).thenReturn(true);
        doThrow(new RuntimeException("downstream unavailable")).when(outboxEventHandler).handle(event);

        outboxDispatchService.dispatchOne(event);

        assertEquals(OutboxEventStatus.PENDING, event.getStatus());
        assertEquals(1, event.getAttempts());
        assertEquals("downstream unavailable", event.getLastError());
        assertNull(event.getLockedBy());
        assertTrue(event.getAvailableAt().isAfter(LocalDateTime.now()), "retry must be scheduled in the future");
    }

    @Test
    void shouldDeadLetterAfterExhaustingMaxAttempts() throws Exception {
        OutboxEvent event = claimedEvent();
        event.setAttempts(2); // this failure will be the 3rd, hitting maxAttempts=3
        when(outboxEventHandler.supports(event.getEventType())).thenReturn(true);
        doThrow(new RuntimeException("still failing")).when(outboxEventHandler).handle(event);

        outboxDispatchService.dispatchOne(event);

        assertEquals(OutboxEventStatus.DEAD_LETTER, event.getStatus());
        assertEquals(3, event.getAttempts());
    }

    @Test
    void shouldTruncateOverlongErrorMessages() throws Exception {
        OutboxEvent event = claimedEvent();
        String longMessage = "x".repeat(2500);
        when(outboxEventHandler.supports(event.getEventType())).thenReturn(true);
        doThrow(new RuntimeException(longMessage)).when(outboxEventHandler).handle(event);

        outboxDispatchService.dispatchOne(event);

        assertEquals(2000, event.getLastError().length());
    }

    @Test
    void shouldRouteToFirstSupportingHandlerWhenMultipleHandlersExist() throws Exception {
        OutboxEvent event = claimedEvent();
        OutboxEventHandler nonMatchingHandler = org.mockito.Mockito.mock(OutboxEventHandler.class);
        when(nonMatchingHandler.supports(anyString())).thenReturn(false);
        when(outboxEventHandler.supports(event.getEventType())).thenReturn(true);

        OutboxDispatchService service = new OutboxDispatchService(outboxEventRepository, List.of(nonMatchingHandler, outboxEventHandler));
        ReflectionTestUtils.setField(service, "maxAttempts", 3);

        service.dispatchOne(event);

        verify(nonMatchingHandler, never()).handle(any());
        verify(outboxEventHandler).handle(event);
        assertEquals(OutboxEventStatus.PUBLISHED, event.getStatus());
    }

    @Test
    void shouldApplyFailureWhenNoHandlerSupportsEventType() {
        OutboxEvent event = claimedEvent();
        when(outboxEventHandler.supports(event.getEventType())).thenReturn(false);

        outboxDispatchService.dispatchOne(event);

        assertEquals(OutboxEventStatus.PENDING, event.getStatus());
        assertEquals(1, event.getAttempts());
        assertTrue(event.getLastError().contains("No OutboxEventHandler supports"));
        verify(outboxEventRepository).save(event);
    }

    private OutboxEvent claimedEvent() {
        OutboxEvent event = new OutboxEvent();
        event.setId(1L);
        event.setStatus(OutboxEventStatus.PROCESSING);
        event.setLockedBy("token-1");
        event.setAttempts(0);
        event.setEventType("some.event");
        return event;
    }
}
