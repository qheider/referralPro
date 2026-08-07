package com.actpro.referral.integration.webhook;

import com.actpro.referral.company.Company;
import com.actpro.referral.company.CompanyRepository;
import com.actpro.referral.company.CompanyStatus;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDateTime;
import java.util.ArrayList;
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
 * Phase 9 hardening: two Section 16 test-matrix requirements proven against a real database
 * rather than mocks - "Concurrent workers do not deliver the same pending item simultaneously"
 * (claim race) and "Duplicate event is safely ignored/acknowledged without duplicate business
 * effects" (redelivery race, the concurrent counterpart to
 * {@code WebhookIngestServiceTest}'s existing sequential duplicate-detection coverage).
 */
@SpringBootTest
@ActiveProfiles("test")
class WebhookEventConcurrentClaimTest {

    @Autowired
    private WebhookProcessingService webhookProcessingService;

    @Autowired
    private WebhookEventRepository webhookEventRepository;

    @Autowired
    private CompanyRepository companyRepository;

    @Autowired
    private PlatformTransactionManager transactionManager;

    private Long companyId;
    private final List<Long> createdEventIds = new ArrayList<>();

    @AfterEach
    void cleanUp() {
        createdEventIds.forEach(id -> webhookEventRepository.findById(id).ifPresent(webhookEventRepository::delete));
        if (companyId != null) {
            companyRepository.findById(companyId).ifPresent(companyRepository::delete);
        }
    }

    private Company newCompany() {
        Company company = new Company();
        company.setName("Concurrency Test Co " + UUID.randomUUID());
        company.setEmail("concurrency-" + UUID.randomUUID() + "@example.test");
        company.setStatus(CompanyStatus.ACTIVE);
        company = companyRepository.save(company);
        companyId = company.getId();
        return company;
    }

    @Test
    void concurrentClaimBatchNeverDoubleClaimsOrDropsAnEvent() throws Exception {
        Company company = newCompany();

        int totalEvents = 20;
        for (int i = 0; i < totalEvents; i++) {
            WebhookEvent event = new WebhookEvent();
            event.setCompany(company);
            event.setEventId("evt_" + UUID.randomUUID());
            event.setEventType("SERVICE_COMPLETED");
            event.setStatus(WebhookEventStatus.RECEIVED);
            event.setRawPayload("{}");
            event.setMaxAttempts(5);
            event.setAvailableAt(LocalDateTime.now().minusSeconds(1));
            event = webhookEventRepository.save(event);
            createdEventIds.add(event.getId());
        }

        Callable<List<WebhookEvent>> claimTask = () -> webhookProcessingService.claimBatch(UUID.randomUUID().toString(), totalEvents);

        ExecutorService pool = Executors.newFixedThreadPool(2);
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch go = new CountDownLatch(1);
        try {
            Callable<List<WebhookEvent>> synchronizedTask = () -> {
                ready.countDown();
                go.await();
                return claimTask.call();
            };
            Future<List<WebhookEvent>> future1 = pool.submit(synchronizedTask);
            Future<List<WebhookEvent>> future2 = pool.submit(synchronizedTask);
            ready.await(5, TimeUnit.SECONDS);
            go.countDown();

            List<WebhookEvent> claimedByWorker1 = future1.get(10, TimeUnit.SECONDS);
            List<WebhookEvent> claimedByWorker2 = future2.get(10, TimeUnit.SECONDS);

            Set<Long> ids1 = claimedByWorker1.stream().map(WebhookEvent::getId).collect(Collectors.toCollection(HashSet::new));
            Set<Long> ids2 = claimedByWorker2.stream().map(WebhookEvent::getId).collect(Collectors.toCollection(HashSet::new));

            assertTrue(ids1.stream().noneMatch(ids2::contains), "No event should be claimed by both workers: " + ids1 + " / " + ids2);
            assertEquals(totalEvents, ids1.size() + ids2.size(), "Every received event must be claimed exactly once across both workers");
        } finally {
            pool.shutdownNow();
        }
    }

    @Test
    void concurrentRedeliveryOfTheSameEventIdInsertsExactlyOneRow() throws Exception {
        Company company = newCompany();
        String eventId = "evt_race_" + UUID.randomUUID();
        TransactionTemplate transactionTemplate = new TransactionTemplate(transactionManager);

        // WebhookEventRepository.insertIgnoreDuplicate is a plain @Modifying query method with no
        // @Transactional of its own (same as claimBatch above) - in production it always runs
        // inside WebhookIngestService#ingest's @Transactional. Calling it directly from a bare
        // thread here needs the same wrapping, or Spring Data rejects it outright.
        Callable<Integer> insertTask = () -> transactionTemplate.execute(status -> webhookEventRepository.insertIgnoreDuplicate(
                companyId, eventId, "SERVICE_COMPLETED", "{}", 5, LocalDateTime.now()));

        ExecutorService pool = Executors.newFixedThreadPool(2);
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch go = new CountDownLatch(1);
        try {
            Callable<Integer> synchronizedTask = () -> {
                ready.countDown();
                go.await();
                return insertTask.call();
            };
            Future<Integer> future1 = pool.submit(synchronizedTask);
            Future<Integer> future2 = pool.submit(synchronizedTask);
            ready.await(5, TimeUnit.SECONDS);
            go.countDown();

            int inserted1 = future1.get(10, TimeUnit.SECONDS);
            int inserted2 = future2.get(10, TimeUnit.SECONDS);

            assertEquals(1, inserted1 + inserted2, "Exactly one of the two concurrent redeliveries must win the insert");

            List<WebhookEvent> matching = webhookEventRepository.findByCompanyIdOrderByCreatedAtDesc(companyId, org.springframework.data.domain.Limit.of(10));
            createdEventIds.addAll(matching.stream().map(WebhookEvent::getId).toList());
            assertEquals(1, matching.size(), "Exactly one webhook_events row must exist for the racing eventId");
        } finally {
            pool.shutdownNow();
        }
    }
}
