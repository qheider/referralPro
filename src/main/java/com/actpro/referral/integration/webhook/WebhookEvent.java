package com.actpro.referral.integration.webhook;

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
 * Durable record of an inbound company webhook, stored by {@link WebhookIngestService} the moment
 * signature verification succeeds - before any business processing. {@link WebhookProcessingService}
 * (async, via {@link WebhookDispatcher}) later matches it to a Referral and applies the
 * configured status mapping. {@code rawPayload}'s revenueAmount/currency fields are stored
 * verbatim for Phase 8 - this phase never parses or acts on them.
 */
@Entity
@Table(
        name = "webhook_events",
        uniqueConstraints = @UniqueConstraint(name = "uk_webhook_events_company_event", columnNames = {"company_id", "event_id"})
)
@Getter
@Setter
@NoArgsConstructor
public class WebhookEvent extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "company_id", nullable = false)
    private Company company;

    @Column(name = "event_id", nullable = false, length = 100)
    private String eventId;

    @Column(name = "event_type", nullable = false, length = 100)
    private String eventType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private WebhookEventStatus status = WebhookEventStatus.RECEIVED;

    @Column(name = "raw_payload", nullable = false, columnDefinition = "TEXT")
    private String rawPayload;

    // Plain id, not a JPA relationship - same "re-fetch via existing API rather than couple
    // entities" convention as ApiSubmission.aggregateId.
    @Column(name = "matched_referral_id")
    private Long matchedReferralId;

    @Column(name = "mapped_status", length = 30)
    private String mappedStatus;

    @Column(nullable = false)
    private int attempts = 0;

    @Column(name = "max_attempts", nullable = false)
    private int maxAttempts;

    @Column(name = "available_at", nullable = false)
    private LocalDateTime availableAt;

    @Column(name = "locked_by", length = 64)
    private String lockedBy;

    @Column(name = "failure_reason", length = 2000)
    private String failureReason;

    @Column(name = "processed_at")
    private LocalDateTime processedAt;
}
