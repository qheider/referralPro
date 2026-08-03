package com.actpro.referral.outbox;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OutboxDispatcherTest {

    @Mock
    private OutboxDispatchService outboxDispatchService;

    @InjectMocks
    private OutboxDispatcher outboxDispatcher;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(outboxDispatcher, "batchSize", 50);
    }

    @Test
    void shouldDoNothingWhenNothingIsClaimed() {
        when(outboxDispatchService.claimBatch(anyString(), anyInt())).thenReturn(List.of());

        outboxDispatcher.dispatchPendingEvents();

        verify(outboxDispatchService, never()).dispatchOne(any());
    }

    @Test
    void shouldDispatchEveryClaimedEvent() {
        OutboxEvent first = new OutboxEvent();
        first.setId(1L);
        OutboxEvent second = new OutboxEvent();
        second.setId(2L);
        when(outboxDispatchService.claimBatch(anyString(), anyInt())).thenReturn(List.of(first, second));

        outboxDispatcher.dispatchPendingEvents();

        ArgumentCaptor<OutboxEvent> captor = ArgumentCaptor.forClass(OutboxEvent.class);
        verify(outboxDispatchService, times(2)).dispatchOne(captor.capture());
        assertEquals(List.of(1L, 2L), captor.getAllValues().stream().map(OutboxEvent::getId).toList());
    }

    @Test
    void shouldUseConfiguredBatchSizeWhenClaiming() {
        ReflectionTestUtils.setField(outboxDispatcher, "batchSize", 25);
        when(outboxDispatchService.claimBatch(anyString(), anyInt())).thenReturn(List.of());

        outboxDispatcher.dispatchPendingEvents();

        verify(outboxDispatchService).claimBatch(anyString(), org.mockito.ArgumentMatchers.eq(25));
    }
}
