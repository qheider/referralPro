package com.actpro.referral.outbox;

import com.actpro.referral.company.Company;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * Write-side API for appending outbox rows. {@code Propagation.MANDATORY} enforces the core
 * transactional-outbox invariant at the type level: this must be called from within a caller's
 * own {@code @Transactional} business method, so the event row commits atomically with the state
 * change it records - never as a separate step after that transaction has already committed.
 */
@Service
@RequiredArgsConstructor
public class OutboxEventPublisher {

    private final OutboxEventRepository outboxEventRepository;
    private final ObjectMapper objectMapper;

    @Transactional(propagation = Propagation.MANDATORY)
    public void publish(Company company, String aggregateType, Long aggregateId, String eventType, Object payload) {
        OutboxEvent event = new OutboxEvent();
        event.setCompany(company);
        event.setAggregateType(aggregateType);
        event.setAggregateId(aggregateId);
        event.setEventType(eventType);
        event.setPayload(serialize(payload));
        event.setStatus(OutboxEventStatus.PENDING);
        event.setAvailableAt(LocalDateTime.now());
        outboxEventRepository.save(event);
    }

    private String serialize(Object payload) {
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("Outbox payload must be JSON-serializable", e);
        }
    }
}
