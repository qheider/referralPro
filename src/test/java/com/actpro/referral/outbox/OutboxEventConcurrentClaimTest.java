package com.actpro.referral.outbox;

import com.actpro.referral.company.Company;
import com.actpro.referral.company.CompanyRepository;
import com.actpro.referral.company.CompanyStatus;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Phase 9 hardening: proves {@code OutboxEventRepository.claimBatch}'s atomic single-statement
 * UPDATE actually enforces "concurrent workers do not deliver the same pending item simultaneously"
 * (Section 16's outgoing-integration test-matrix requirement) against a real database engine, not
 * a mock - the row-lock semantics the Javadoc on that query claims can't be proven by a Mockito
 * unit test. Boots the real app context against the H2 test profile (MODE=MySQL supports the same
 * UPDATE...ORDER BY...LIMIT syntax), same pattern as {@code webhook.WebhookPublicPathSecurityTest}'s
 * "first full-context test" precedent.
 */
@SpringBootTest
@ActiveProfiles("test")
class OutboxEventConcurrentClaimTest {

    @Autowired
    private OutboxDispatchService outboxDispatchService;

    @Autowired
    private OutboxEventRepository outboxEventRepository;

    @Autowired
    private CompanyRepository companyRepository;

    private Long companyId;
    private final List<Long> createdEventIds = new java.util.ArrayList<>();

    @AfterEach
    void cleanUp() {
        createdEventIds.forEach(id -> outboxEventRepository.findById(id).ifPresent(outboxEventRepository::delete));
        if (companyId != null) {
            companyRepository.findById(companyId).ifPresent(companyRepository::delete);
        }
    }

    @Test
    void concurrentClaimBatchNeverDoubleClaimsOrDropsAnEvent() throws Exception {
        Company company = new Company();
        company.setName("Concurrency Test Co " + UUID.randomUUID());
        company.setEmail("concurrency-" + UUID.randomUUID() + "@example.test");
        company.setStatus(CompanyStatus.ACTIVE);
        company = companyRepository.save(company);
        companyId = company.getId();

        int totalEvents = 20;
        for (int i = 0; i < totalEvents; i++) {
            OutboxEvent event = new OutboxEvent();
            event.setCompany(company);
            event.setAggregateType("REFERRAL");
            event.setAggregateId((long) i);
            event.setEventType("referral.lead_registered");
            event.setPayload("{}");
            event.setStatus(OutboxEventStatus.PENDING);
            event.setAvailableAt(LocalDateTime.now().minusSeconds(1));
            event = outboxEventRepository.save(event);
            createdEventIds.add(event.getId());
        }

        // Both workers race for the *entire* batch (batchSize == totalEvents) so a real collision
        // is forced if the claim isn't actually atomic/serialized at the row level.
        Callable<List<OutboxEvent>> claimTask = () -> outboxDispatchService.claimBatch(UUID.randomUUID().toString(), totalEvents);

        ExecutorService pool = Executors.newFixedThreadPool(2);
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch go = new CountDownLatch(1);
        try {
            Callable<List<OutboxEvent>> synchronizedTask = () -> {
                ready.countDown();
                go.await();
                return claimTask.call();
            };
            Future<List<OutboxEvent>> future1 = pool.submit(synchronizedTask);
            Future<List<OutboxEvent>> future2 = pool.submit(synchronizedTask);
            ready.await(5, TimeUnit.SECONDS);
            go.countDown();

            List<OutboxEvent> claimedByWorker1 = future1.get(10, TimeUnit.SECONDS);
            List<OutboxEvent> claimedByWorker2 = future2.get(10, TimeUnit.SECONDS);

            Set<Long> ids1 = claimedByWorker1.stream().map(OutboxEvent::getId).collect(Collectors.toCollection(HashSet::new));
            Set<Long> ids2 = claimedByWorker2.stream().map(OutboxEvent::getId).collect(Collectors.toCollection(HashSet::new));

            assertTrue(ids1.stream().noneMatch(ids2::contains), "No event should be claimed by both workers: " + ids1 + " / " + ids2);
            assertEquals(totalEvents, ids1.size() + ids2.size(), "Every pending event must be claimed exactly once across both workers");
        } finally {
            pool.shutdownNow();
        }
    }
}
