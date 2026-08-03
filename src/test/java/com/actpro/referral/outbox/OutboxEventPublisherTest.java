package com.actpro.referral.outbox;

import com.actpro.referral.company.Company;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OutboxEventPublisherTest {

    @Mock
    private OutboxEventRepository outboxEventRepository;

    private OutboxEventPublisher outboxEventPublisher;

    @BeforeEach
    void setUp() {
        outboxEventPublisher = new OutboxEventPublisher(outboxEventRepository, new ObjectMapper());
    }

    @Test
    void shouldSerializePayloadAndSaveAPendingEvent() {
        Company company = new Company();
        company.setId(5L);
        when(outboxEventRepository.save(any(OutboxEvent.class))).thenAnswer(invocation -> invocation.getArgument(0));

        outboxEventPublisher.publish(company, "REFERRAL", 42L, "referral.created", new PayloadStub("abc"));

        ArgumentCaptor<OutboxEvent> captor = ArgumentCaptor.forClass(OutboxEvent.class);
        verify(outboxEventRepository).save(captor.capture());
        OutboxEvent saved = captor.getValue();

        assertEquals(company, saved.getCompany());
        assertEquals("REFERRAL", saved.getAggregateType());
        assertEquals(42L, saved.getAggregateId());
        assertEquals("referral.created", saved.getEventType());
        assertEquals(OutboxEventStatus.PENDING, saved.getStatus());
        assertNotNull(saved.getAvailableAt());
        assertTrue(saved.getPayload().contains("\"code\":\"abc\""));
    }

    @Test
    void shouldRejectNonSerializablePayload() {
        Company company = new Company();
        company.setId(5L);

        // A bare Object has no properties for Jackson to discover, and FAIL_ON_EMPTY_BEANS is on
        // by default, so this reliably triggers the serialization-failure path being tested.
        assertThrowsIllegalArgument(() ->
                outboxEventPublisher.publish(company, "REFERRAL", 42L, "referral.created", new Object()));
    }

    private void assertThrowsIllegalArgument(Runnable runnable) {
        try {
            runnable.run();
        } catch (IllegalArgumentException expected) {
            return;
        }
        throw new AssertionError("Expected IllegalArgumentException to be thrown");
    }

    private record PayloadStub(String code) {
    }
}
