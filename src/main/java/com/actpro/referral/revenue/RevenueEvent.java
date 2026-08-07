package com.actpro.referral.revenue;

import com.actpro.referral.auth.DashboardUser;
import com.actpro.referral.campaign.Campaign;
import com.actpro.referral.common.BaseEntity;
import com.actpro.referral.company.Company;
import com.actpro.referral.referral.Referral;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Durable, idempotent record of "this ambassador-attributed referral reached a qualifying
 * status" - the Phase 8 counterpart to the legacy {@code reward.Reward}/{@code Conversion} pair,
 * which only ever applied to the direct-API PlatformUser-referrer flow. Created/reversed by
 * {@link RevenueEventService} reacting to the {@code referral.status_changed} outbox event
 * {@code WebhookProcessingService} publishes (Phase 7 deliberately left revenueAmount/currency
 * unparsed for this phase - see {@code IncomingServiceStatusPayload}'s Javadoc history).
 * <p>
 * {@code uk_revenue_events_referral} is the idempotency key: at most one row per {@link Referral},
 * regardless of how many times its qualifying webhook is redelivered/reprocessed.
 */
@Entity
@Table(
        name = "revenue_events",
        uniqueConstraints = @UniqueConstraint(name = "uk_revenue_events_referral", columnNames = "referral_id")
)
@Getter
@Setter
@NoArgsConstructor
public class RevenueEvent extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "company_id", nullable = false)
    private Company company;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "campaign_id", nullable = false)
    private Campaign campaign;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "referral_id", nullable = false)
    private Referral referral;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ambassador_user_id", nullable = false)
    private DashboardUser ambassadorUser;

    // The ReferralStatus name whose transition first triggered this event (COMPLETED or
    // CONVERTED under the default reward-mapping policy - see RewardStatusMappingService).
    @Column(name = "qualifying_status", nullable = false, length = 30)
    private String qualifyingStatus;

    @Column(name = "occurred_at", nullable = false)
    private LocalDateTime occurredAt;

    // Informational only - never feeds AmbassadorReward.rewardValue. See the migration's comment.
    @Column(name = "revenue_amount", precision = 12, scale = 2)
    private BigDecimal revenueAmount;

    @Column(length = 10)
    private String currency;

    @Column(name = "currency_mismatch", nullable = false)
    private boolean currencyMismatch = false;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private RevenueEventStatus status = RevenueEventStatus.RECORDED;

    @Column(name = "reversed_at")
    private LocalDateTime reversedAt;
}
