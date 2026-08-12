package com.actpro.referral.integration;

import org.springframework.data.domain.Limit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface ApiSubmissionRepository extends JpaRepository<ApiSubmission, Long> {

    // Idempotency guard backed by uk_api_submissions_idempotency - re-processing the same outbox
    // row must find the existing submission rather than create a duplicate.
    Optional<ApiSubmission> findByCompanyIdAndAggregateTypeAndAggregateIdAndSourceEventType(
            Long companyId, String aggregateType, Long aggregateId, String sourceEventType);

    // Atomic single-statement claim, same rationale as OutboxEventRepository.claimBatch: MySQL
    // row-locks as it updates, so two dispatcher instances can't claim the same row. Restricted to
    // companies whose integration is currently ACTIVE so submissions for a DISABLED/ERROR/
    // PENDING_VERIFICATION company simply age unclaimed instead of being hammered - a plain
    // UPDATE...JOIN can't combine with ORDER BY/LIMIT in MySQL, hence the subquery.
    @Modifying(clearAutomatically = true)
    @Query(value = """
            UPDATE api_submissions
            SET status = 'PROCESSING', locked_by = :claimToken, updated_at = CURRENT_TIMESTAMP
            WHERE status IN ('PENDING', 'RETRY_SCHEDULED') AND available_at <= :now
              AND company_id IN (SELECT company_id FROM company_integrations WHERE status = 'ACTIVE')
            ORDER BY available_at ASC
            LIMIT :batchSize
            """, nativeQuery = true)
    int claimBatch(@Param("claimToken") String claimToken, @Param("now") LocalDateTime now, @Param("batchSize") int batchSize);

    List<ApiSubmission> findByLockedBy(String lockedBy);

    List<ApiSubmission> findByCompanyIdAndStatusOrderByCreatedAtDesc(Long companyId, ApiSubmissionStatus status, Limit limit);

    List<ApiSubmission> findByCompanyIdOrderByCreatedAtDesc(Long companyId, Limit limit);

    Optional<ApiSubmission> findByIdAndCompanyId(Long id, Long companyId);

    // Reference-matching for Phase 7 webhook processing: resolves which Referral (via
    // ApiSubmission.aggregateId) a webhook's serviceReference/companyUserReference refers to.
    // serviceReference is matched against companyTransactionReference first (more specific);
    // companyUserReference against companyCustomerReference is the fallback.
    Optional<ApiSubmission> findByCompanyIdAndCompanyTransactionReference(Long companyId, String companyTransactionReference);

    Optional<ApiSubmission> findByCompanyIdAndCompanyCustomerReference(Long companyId, String companyCustomerReference);
}
