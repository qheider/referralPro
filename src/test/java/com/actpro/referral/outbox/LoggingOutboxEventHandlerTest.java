package com.actpro.referral.outbox;

import org.junit.jupiter.api.Test;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.core.annotation.Order;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LoggingOutboxEventHandlerTest {

    private final LoggingOutboxEventHandler handler = new LoggingOutboxEventHandler();

    @Test
    void shouldSupportAnyEventTypeAsCatchAll() {
        assertTrue(handler.supports("referral.lead_registered"));
        assertTrue(handler.supports("some.unknown.future.event"));
    }

    @Test
    void shouldBeOrderedLastSoMoreSpecificHandlersWinFirst() {
        Order order = AnnotationUtils.findAnnotation(LoggingOutboxEventHandler.class, Order.class);
        assertEquals(Ordered.LOWEST_PRECEDENCE, order.value());
    }

    @Test
    void shouldHandleWithoutThrowing() {
        OutboxEvent event = new OutboxEvent();
        event.setId(1L);
        event.setEventType("anything");
        event.setAggregateType("REFERRAL");
        event.setAggregateId(2L);

        assertDoesNotThrow(() -> handler.handle(event));
    }
}
