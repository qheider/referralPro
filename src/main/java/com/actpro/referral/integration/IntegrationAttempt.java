package com.actpro.referral.integration;

import com.actpro.referral.common.BaseEntity;
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
 * One row per HTTP delivery attempt against an {@link ApiSubmission} - the durable attempt
 * history the admin monitoring UI renders (timestamps, outcome, HTTP status, failure category,
 * sanitized message, next retry time).
 */
@Entity
@Table(
        name = "integration_attempts",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_integration_attempts_submission_attempt",
                columnNames = {"api_submission_id", "attempt_number"})
)
@Getter
@Setter
@NoArgsConstructor
public class IntegrationAttempt extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "api_submission_id", nullable = false)
    private ApiSubmission apiSubmission;

    @Column(name = "attempt_number", nullable = false)
    private int attemptNumber;

    @Column(name = "started_at", nullable = false)
    private LocalDateTime startedAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @Column(name = "http_status")
    private Integer httpStatus;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AttemptOutcome outcome;

    @Enumerated(EnumType.STRING)
    @Column(name = "failure_category", length = 30)
    private FailureCategory failureCategory;

    @Column(name = "sanitized_message", length = 2000)
    private String sanitizedMessage;

    @Column(name = "next_retry_at")
    private LocalDateTime nextRetryAt;
}
