package com.actpro.referral.outbox;

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
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "outbox_events")
@Getter
@Setter
@NoArgsConstructor
public class OutboxEvent extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "company_id", nullable = false)
    private Company company;

    // The domain entity this event describes, e.g. "REFERRAL", "AMBASSADOR" - lets a future
    // consumer filter/route without parsing the payload.
    @Column(name = "aggregate_type", nullable = false, length = 50)
    private String aggregateType;

    @Column(name = "aggregate_id", nullable = false)
    private Long aggregateId;

    // e.g. "referral.created" - the wire event name a consumer subscribes to.
    @Column(name = "event_type", nullable = false, length = 100)
    private String eventType;

    // JSON. Prefer ids the consumer can re-fetch via the existing API over duplicating derived
    // state that could drift from the service that produced the event.
    @Column(columnDefinition = "TEXT", nullable = false)
    private String payload;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private OutboxEventStatus status = OutboxEventStatus.PENDING;

    @Column(nullable = false)
    private int attempts = 0;

    // Not eligible for (re)dispatch until this time - set to "now" on first insert, pushed
    // forward by the dispatcher's backoff on each retry.
    @Column(name = "available_at", nullable = false)
    private LocalDateTime availableAt;

    @Column(name = "published_at")
    private LocalDateTime publishedAt;

    // Set to a per-dispatch-run token while PROCESSING, so the worker that claimed a row can find
    // exactly the rows it claimed without re-querying by status (which could race with another
    // worker's concurrent claim). Cleared once the row leaves PROCESSING.
    @Column(name = "locked_by", length = 64)
    private String lockedBy;

    @Column(name = "last_error", length = 2000)
    private String lastError;
}
