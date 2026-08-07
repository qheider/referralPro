package com.actpro.referral.revenue;

import com.actpro.referral.auth.DashboardUser;
import com.actpro.referral.campaign.Campaign;
import com.actpro.referral.campaign.RewardType;
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
 * The payable reward calculated from a {@link RevenueEvent}, one-to-one
 * ({@code uk_ambassador_rewards_revenue_event}). {@code rewardType}/{@code rewardValue} are a
 * snapshot of {@code Campaign.rewardType}/{@code Campaign.referrerRewardValue} taken by
 * {@link RevenueEventService} at creation time - a later campaign edit (already field-locked to
 * DRAFT-only by {@code CampaignService}) can never rewrite a historical reward's payout amount.
 * See {@link AmbassadorRewardStatus} for the lifecycle.
 */
@Entity
@Table(
        name = "ambassador_rewards",
        uniqueConstraints = @UniqueConstraint(name = "uk_ambassador_rewards_revenue_event", columnNames = "revenue_event_id")
)
@Getter
@Setter
@NoArgsConstructor
public class AmbassadorReward extends BaseEntity {

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
    @JoinColumn(name = "revenue_event_id", nullable = false)
    private RevenueEvent revenueEvent;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ambassador_user_id", nullable = false)
    private DashboardUser ambassadorUser;

    @Enumerated(EnumType.STRING)
    @Column(name = "reward_type", nullable = false, length = 30)
    private RewardType rewardType;

    @Column(name = "reward_value", nullable = false, precision = 10, scale = 2)
    private BigDecimal rewardValue;

    @Column(length = 10)
    private String currency;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AmbassadorRewardStatus status = AmbassadorRewardStatus.PENDING;

    // e.g. "CURRENCY_MISMATCH" - why a PENDING reward hasn't auto-promoted to ELIGIBLE.
    @Column(name = "hold_reason", length = 100)
    private String holdReason;

    @Column(name = "approved_at")
    private LocalDateTime approvedAt;

    @Column(name = "paid_at")
    private LocalDateTime paidAt;

    @Column(name = "rejected_at")
    private LocalDateTime rejectedAt;

    @Column(name = "reversed_at")
    private LocalDateTime reversedAt;

    @Column(name = "rejection_reason", length = 500)
    private String rejectionReason;
}
