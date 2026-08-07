package com.actpro.referral.integration;

import com.actpro.referral.company.CompanyIntegration;
import com.actpro.referral.company.CompanyIntegrationRepository;
import com.actpro.referral.company.CompanyIntegrationStatus;
import com.actpro.referral.integration.dto.CreateUserApiCallResult;
import com.actpro.referral.integration.dto.CreateUserApiRequestPayload;
import com.actpro.referral.referral.Referral;
import com.actpro.referral.referral.ReferralRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Stage 2 of the two-stage outbound delivery pipeline: transactional halves of a dispatch cycle
 * for {@link ApiSubmission} rows, kept separate from {@link ApiSubmissionDispatcher}'s
 * {@code @Scheduled} entry point for the same self-invocation reason as
 * {@code outbox.OutboxDispatchService}. Re-fetches the {@link Referral} at dispatch time rather
 * than duplicating derived state in the submission row (same convention {@code OutboxEvent}'s
 * Javadoc documents). Applies bounded exponential backoff <em>with jitter</em> - distinct from
 * {@code outbox.OutboxDispatchService.backoffSeconds}, which has none.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ApiSubmissionDispatchService {

    private static final Set<FailureCategory> TRANSIENT_CATEGORIES =
            Set.of(FailureCategory.TIMEOUT, FailureCategory.CONNECTION_ERROR, FailureCategory.RATE_LIMITED, FailureCategory.SERVER_ERROR);
    private static final int MAX_BACKOFF_SECONDS = 300;

    private final ApiSubmissionRepository apiSubmissionRepository;
    private final IntegrationAttemptRepository integrationAttemptRepository;
    private final CompanyIntegrationRepository companyIntegrationRepository;
    private final ReferralRepository referralRepository;
    private final CreateUserApiClient createUserApiClient;

    @Transactional
    public List<ApiSubmission> claimBatch(String claimToken, int batchSize) {
        int claimed = apiSubmissionRepository.claimBatch(claimToken, LocalDateTime.now(), batchSize);
        if (claimed == 0) {
            return List.of();
        }
        return apiSubmissionRepository.findByLockedBy(claimToken);
    }

    @Transactional
    public void dispatchOne(ApiSubmission submission) {
        LocalDateTime startedAt = LocalDateTime.now();
        int attemptNumber = submission.getAttempts() + 1;

        Optional<CompanyIntegration> integrationOpt = companyIntegrationRepository.findByCompanyId(submission.getCompany().getId());
        Optional<Referral> referralOpt = integrationOpt.isPresent()
                ? referralRepository.findById(submission.getAggregateId())
                : Optional.empty();

        Integer httpStatus = null;
        FailureCategory category;
        String message;
        boolean success = false;
        String companyCustomerReference = null;
        String companyTransactionReference = null;

        if (integrationOpt.isEmpty()) {
            // Shouldn't normally happen - the claim query only pulls rows for companies whose
            // integration is currently ACTIVE - but guard defensively (e.g. a race with disable).
            category = FailureCategory.CONNECTION_ERROR;
            message = "Company integration configuration not found";
        } else if (referralOpt.isEmpty()) {
            // The referral this submission was created for no longer exists - not something a
            // retry can fix.
            category = FailureCategory.CLIENT_ERROR;
            message = "Referral " + submission.getAggregateId() + " no longer exists";
        } else {
            CompanyIntegration integration = integrationOpt.get();
            CreateUserApiRequestPayload payload = buildPayload(submission, referralOpt.get());
            CreateUserApiCallResult result = createUserApiClient.call(integration, payload);

            if (result.ioSuccess()) {
                httpStatus = result.httpStatus();
                if (httpStatus != null && httpStatus < 400) {
                    success = true;
                    category = FailureCategory.NONE;
                    message = null;
                    companyCustomerReference = result.companyCustomerReference();
                    companyTransactionReference = result.companyTransactionReference();
                } else {
                    category = categorizeHttpStatus(httpStatus);
                    message = "Company API returned HTTP " + httpStatus;
                }
            } else {
                category = result.ioFailureCategory();
                message = result.sanitizedErrorMessage();
            }

            if (category == FailureCategory.AUTH_ERROR) {
                integration.setStatus(CompanyIntegrationStatus.ERROR);
                companyIntegrationRepository.save(integration);
                log.warn("Company {} integration flipped to ERROR after an AUTH_ERROR delivering submission {}",
                        submission.getCompany().getId(), submission.getId());
            }
        }

        LocalDateTime completedAt = LocalDateTime.now();
        submission.setAttempts(attemptNumber);
        submission.setLockedBy(null);
        submission.setLastResponseStatus(httpStatus);
        submission.setLastError(success ? null : message);
        if (success) {
            submission.setCompanyCustomerReference(companyCustomerReference);
            submission.setCompanyTransactionReference(companyTransactionReference);
        }

        LocalDateTime nextRetryAt = null;
        if (success) {
            submission.setStatus(ApiSubmissionStatus.SUCCEEDED);
            submission.setSubmittedAt(completedAt);
        } else if (TRANSIENT_CATEGORIES.contains(category) && attemptNumber < submission.getMaxAttempts()) {
            nextRetryAt = completedAt.plusSeconds(backoffSecondsWithJitter(attemptNumber));
            submission.setStatus(ApiSubmissionStatus.RETRY_SCHEDULED);
            submission.setAvailableAt(nextRetryAt);
        } else {
            submission.setStatus(ApiSubmissionStatus.PERMANENTLY_FAILED);
        }

        apiSubmissionRepository.save(submission);
        recordAttempt(submission, attemptNumber, startedAt, completedAt, httpStatus,
                success ? AttemptOutcome.SUCCESS : AttemptOutcome.FAILURE, category, message, nextRetryAt);
    }

    private CreateUserApiRequestPayload buildPayload(ApiSubmission submission, Referral referral) {
        return new CreateUserApiRequestPayload(
                submission.getExternalRequestId(),
                referral.getCustomerUser() != null ? referral.getCustomerUser().getName() : null,
                referral.getCustomerUser() != null ? referral.getCustomerUser().getEmail() : null,
                referral.getCampaign() != null ? referral.getCampaign().getCampaignCode() : null,
                referral.getReferralCode()
        );
    }

    private FailureCategory categorizeHttpStatus(int httpStatus) {
        if (httpStatus == 401 || httpStatus == 403) {
            return FailureCategory.AUTH_ERROR;
        }
        if (httpStatus == 429) {
            return FailureCategory.RATE_LIMITED;
        }
        // Unlisted 5xx (not just 502/503/504) are treated as transient too - a company-side bug
        // shouldn't permanently abandon a submission after a single attempt.
        if (httpStatus >= 500) {
            return FailureCategory.SERVER_ERROR;
        }
        return FailureCategory.CLIENT_ERROR;
    }

    private long backoffSecondsWithJitter(int attemptNumber) {
        double capped = Math.min(MAX_BACKOFF_SECONDS, Math.pow(2, attemptNumber));
        double jitterFactor = 0.5 + ThreadLocalRandom.current().nextDouble() * 0.5; // [0.5, 1.0)
        return Math.max(1, Math.round(capped * jitterFactor));
    }

    private void recordAttempt(
            ApiSubmission submission, int attemptNumber, LocalDateTime startedAt, LocalDateTime completedAt,
            Integer httpStatus, AttemptOutcome outcome, FailureCategory failureCategory, String sanitizedMessage,
            LocalDateTime nextRetryAt) {
        IntegrationAttempt attempt = new IntegrationAttempt();
        attempt.setApiSubmission(submission);
        attempt.setAttemptNumber(attemptNumber);
        attempt.setStartedAt(startedAt);
        attempt.setCompletedAt(completedAt);
        attempt.setHttpStatus(httpStatus);
        attempt.setOutcome(outcome);
        attempt.setFailureCategory(failureCategory);
        attempt.setSanitizedMessage(truncate(sanitizedMessage));
        attempt.setNextRetryAt(nextRetryAt);
        integrationAttemptRepository.save(attempt);
    }

    private String truncate(String message) {
        if (message == null) {
            return null;
        }
        return message.length() > 2000 ? message.substring(0, 2000) : message;
    }
}
