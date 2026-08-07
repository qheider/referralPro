package com.actpro.referral.integration;

import com.actpro.referral.company.CompanyIntegrationRepository;
import com.actpro.referral.outbox.OutboxEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Creates stage-1 {@link ApiSubmission} rows from a durable outbox event. Idempotent: re-invoking
 * with the same (company, aggregateType, aggregateId, sourceEventType) - e.g. because the outbox
 * row was reprocessed after a crash - returns the existing row instead of creating a duplicate,
 * backed by {@code uk_api_submissions_idempotency}. Always creates a row regardless of the
 * company's current integration status/existence, so a company that configures integration
 * *after* a lead already registered doesn't lose that submission - dispatch eligibility is
 * enforced later, at claim time, not here.
 */
@Service
@RequiredArgsConstructor
public class ApiSubmissionService {

    private final ApiSubmissionRepository apiSubmissionRepository;
    private final CompanyIntegrationRepository companyIntegrationRepository;

    @Value("${app.integration.default-max-retry-attempts:5}")
    private int defaultMaxRetryAttempts;

    @Transactional
    public ApiSubmission createOrFindSubmission(OutboxEvent event) {
        Long companyId = event.getCompany().getId();
        return apiSubmissionRepository
                .findByCompanyIdAndAggregateTypeAndAggregateIdAndSourceEventType(
                        companyId, event.getAggregateType(), event.getAggregateId(), event.getEventType())
                .orElseGet(() -> create(event));
    }

    private ApiSubmission create(OutboxEvent event) {
        int maxAttempts = companyIntegrationRepository.findByCompanyId(event.getCompany().getId())
                .map(integration -> integration.getMaxRetryAttempts())
                .orElse(defaultMaxRetryAttempts);

        ApiSubmission submission = new ApiSubmission();
        submission.setCompany(event.getCompany());
        submission.setAggregateType(event.getAggregateType());
        submission.setAggregateId(event.getAggregateId());
        submission.setSourceEventType(event.getEventType());
        submission.setExternalRequestId("req_" + UUID.randomUUID());
        submission.setStatus(ApiSubmissionStatus.PENDING);
        submission.setMaxAttempts(maxAttempts);
        submission.setAvailableAt(LocalDateTime.now());
        return apiSubmissionRepository.save(submission);
    }
}
