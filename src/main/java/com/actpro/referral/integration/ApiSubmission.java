package com.actpro.referral.integration;

import com.actpro.referral.common.BaseEntity;
import com.actpro.referral.company.Company;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * Stage 1 of the two-stage outbound delivery pipeline: a durable, idempotent record of "this
 * domain event needs to reach the company's Create User API", created by
 * {@link ApiSubmissionService#createOrFindSubmission} the moment the {@code referral.lead_registered}
 * outbox event is handled - before any HTTP call is attempted. Stage 2
 * ({@link ApiSubmissionDispatcher}/{@link ApiSubmissionDispatchService}) claims and delivers these
 * rows independently, with its own retry/backoff lifecycle, decoupled from the outbox's own
 * (coarser, non-HTTP-aware) retry loop.
 */
@Entity
@Table(
        name = "api_submissions",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_api_submissions_idempotency",
                        columnNames = {"company_id", "aggregate_type", "aggregate_id", "source_event_type"}),
                @UniqueConstraint(name = "uk_api_submissions_external_request_id", columnNames = "external_request_id")
        }
)
@Getter
@Setter
@NoArgsConstructor
public class ApiSubmission extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "company_id", nullable = false)
    private Company company;

    @Column(name = "aggregate_type", nullable = false, length = 50)
    private String aggregateType;

    @Column(name = "aggregate_id", nullable = false)
    private Long aggregateId;

    @Column(name = "source_event_type", nullable = false, length = 100)
    private String sourceEventType;

    // Sent to the company API as externalRequestId - the idempotency key on their side.
    @Column(name = "external_request_id", nullable = false, length = 64)
    private String externalRequestId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private ApiSubmissionStatus status = ApiSubmissionStatus.PENDING;

    @Column(nullable = false)
    private int attempts = 0;

    // Snapshot of CompanyIntegration.maxRetryAttempts at creation time.
    @Column(name = "max_attempts", nullable = false)
    private int maxAttempts;

    @Column(name = "available_at", nullable = false)
    private LocalDateTime availableAt;

    // Per-dispatch-run claim token, same pattern as OutboxEvent.lockedBy.
    @Column(name = "locked_by", length = 64)
    private String lockedBy;

    @Column(name = "last_error", length = 2000)
    private String lastError;

    @Column(name = "company_customer_reference")
    private String companyCustomerReference;

    @Column(name = "company_transaction_reference")
    private String companyTransactionReference;

    @Column(name = "submitted_at")
    private LocalDateTime submittedAt;

    @Column(name = "last_response_status")
    private Integer lastResponseStatus;
}
