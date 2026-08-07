package com.actpro.referral.integration.webhook;

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
public interface WebhookEventRepository extends JpaRepository<WebhookEvent, Long> {

    // Atomic single-statement claim, same rationale as OutboxEventRepository/ApiSubmissionRepository's
    // claimBatch. Deliberately NOT restricted by company_integrations.status (unlike
    // ApiSubmissionRepository.claimBatch) - disabling outbound delivery shouldn't silently pause
    // inbound webhook-driven status updates, a separate concern.
    @Modifying(clearAutomatically = true)
    @Query(value = """
            UPDATE webhook_events
            SET status = 'PROCESSING', locked_by = :claimToken, updated_at = CURRENT_TIMESTAMP
            WHERE status IN ('RECEIVED', 'RETRY_SCHEDULED') AND available_at <= :now
            ORDER BY available_at ASC
            LIMIT :batchSize
            """, nativeQuery = true)
    int claimBatch(@Param("claimToken") String claimToken, @Param("now") LocalDateTime now, @Param("batchSize") int batchSize);

    // Idempotent redelivery guard via INSERT IGNORE rather than entity save()+catch: MySQL
    // silently skips the row (returns 0 affected) on a uk_webhook_events_company_event
    // violation instead of raising an error, so this never throws - unlike a JPA entity flush
    // hitting the same constraint, which marks the current transaction rollback-only even if the
    // resulting DataIntegrityViolationException is caught in application code (confirmed live:
    // that approach 500'd on a duplicate redelivery instead of the intended idempotent 200 ack,
    // regardless of REQUIRES_NEW - the flush that fails poisons its own transaction either way).
    // Returns 1 if inserted (new event), 0 if ignored (duplicate).
    @Modifying
    @Query(value = """
            INSERT IGNORE INTO webhook_events
                (company_id, event_id, event_type, status, raw_payload, attempts, max_attempts, available_at, created_at, updated_at)
            VALUES
                (:companyId, :eventId, :eventType, 'RECEIVED', :rawPayload, 0, :maxAttempts, :availableAt, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
            """, nativeQuery = true)
    int insertIgnoreDuplicate(
            @Param("companyId") Long companyId,
            @Param("eventId") String eventId,
            @Param("eventType") String eventType,
            @Param("rawPayload") String rawPayload,
            @Param("maxAttempts") int maxAttempts,
            @Param("availableAt") LocalDateTime availableAt);

    List<WebhookEvent> findByLockedBy(String lockedBy);

    List<WebhookEvent> findByCompanyIdAndStatusOrderByCreatedAtDesc(Long companyId, WebhookEventStatus status, Limit limit);

    List<WebhookEvent> findByCompanyIdOrderByCreatedAtDesc(Long companyId, Limit limit);

    Optional<WebhookEvent> findByIdAndCompanyId(Long id, Long companyId);
}
