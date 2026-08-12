package com.actpro.referral.integration;

import com.actpro.referral.company.Company;
import com.actpro.referral.company.CompanyIntegration;
import com.actpro.referral.company.CompanyIntegrationRepository;
import com.actpro.referral.company.CompanyIntegrationStatus;
import com.actpro.referral.company.CompanyRepository;
import com.actpro.referral.company.CompanyStatus;
import com.actpro.referral.integration.webhook.WebhookPublicIdGenerator;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

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
 * Phase 9 hardening: the "Outgoing integration: Concurrent workers do not deliver the same
 * pending item simultaneously" line from Section 16's minimum test matrix, proven against a real
 * database rather than mocks - same rationale as {@code outbox.OutboxEventConcurrentClaimTest}.
 */
@SpringBootTest
@ActiveProfiles("test")
class ApiSubmissionConcurrentClaimTest {

    @Autowired
    private ApiSubmissionDispatchService apiSubmissionDispatchService;

    @Autowired
    private ApiSubmissionRepository apiSubmissionRepository;

    @Autowired
    private CompanyRepository companyRepository;

    @Autowired
    private CompanyIntegrationRepository companyIntegrationRepository;

    @Autowired
    private WebhookPublicIdGenerator webhookPublicIdGenerator;

    private Long companyId;
    private final List<Long> createdSubmissionIds = new ArrayList<>();

    @AfterEach
    void cleanUp() {
        createdSubmissionIds.forEach(id -> apiSubmissionRepository.findById(id).ifPresent(apiSubmissionRepository::delete));
        if (companyId != null) {
            companyIntegrationRepository.findByCompanyId(companyId).ifPresent(companyIntegrationRepository::delete);
            companyRepository.findById(companyId).ifPresent(companyRepository::delete);
        }
    }

    @Test
    void concurrentClaimBatchNeverDoubleClaimsOrDropsASubmission() throws Exception {
        Company company = new Company();
        company.setName("Concurrency Test Co " + UUID.randomUUID());
        company.setEmail("concurrency-" + UUID.randomUUID() + "@example.test");
        company.setStatus(CompanyStatus.ACTIVE);
        company = companyRepository.save(company);
        companyId = company.getId();

        CompanyIntegration integration = new CompanyIntegration();
        integration.setCompany(company);
        integration.setWebhookPublicId(webhookPublicIdGenerator.generateUniqueId());
        integration.setStatus(CompanyIntegrationStatus.ACTIVE);
        companyIntegrationRepository.save(integration);

        int totalSubmissions = 20;
        for (int i = 0; i < totalSubmissions; i++) {
            ApiSubmission submission = new ApiSubmission();
            submission.setCompany(company);
            submission.setAggregateType("REFERRAL");
            submission.setAggregateId((long) i);
            submission.setSourceEventType("referral.lead_registered");
            submission.setExternalRequestId("req_" + UUID.randomUUID());
            submission.setStatus(ApiSubmissionStatus.PENDING);
            submission.setMaxAttempts(5);
            submission.setAvailableAt(LocalDateTime.now().minusSeconds(1));
            submission = apiSubmissionRepository.save(submission);
            createdSubmissionIds.add(submission.getId());
        }

        Callable<List<ApiSubmission>> claimTask =
                () -> apiSubmissionDispatchService.claimBatch(UUID.randomUUID().toString(), totalSubmissions);

        ExecutorService pool = Executors.newFixedThreadPool(2);
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch go = new CountDownLatch(1);
        try {
            Callable<List<ApiSubmission>> synchronizedTask = () -> {
                ready.countDown();
                go.await();
                return claimTask.call();
            };
            Future<List<ApiSubmission>> future1 = pool.submit(synchronizedTask);
            Future<List<ApiSubmission>> future2 = pool.submit(synchronizedTask);
            ready.await(5, TimeUnit.SECONDS);
            go.countDown();

            List<ApiSubmission> claimedByWorker1 = future1.get(10, TimeUnit.SECONDS);
            List<ApiSubmission> claimedByWorker2 = future2.get(10, TimeUnit.SECONDS);

            Set<Long> ids1 = claimedByWorker1.stream().map(ApiSubmission::getId).collect(Collectors.toCollection(HashSet::new));
            Set<Long> ids2 = claimedByWorker2.stream().map(ApiSubmission::getId).collect(Collectors.toCollection(HashSet::new));

            assertTrue(ids1.stream().noneMatch(ids2::contains), "No submission should be claimed by both workers: " + ids1 + " / " + ids2);
            assertEquals(totalSubmissions, ids1.size() + ids2.size(), "Every pending submission must be claimed exactly once across both workers");
        } finally {
            pool.shutdownNow();
        }
    }
}
