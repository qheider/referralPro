package com.actpro.referral.reward;

import com.actpro.referral.campaign.Campaign;
import com.actpro.referral.campaign.RewardType;
import com.actpro.referral.common.BaseEntity;
import com.actpro.referral.company.Company;
import com.actpro.referral.conversion.Conversion;
import com.actpro.referral.user.PlatformUser;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "rewards",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_rewards_conversion_user",
                columnNames = {"conversion_id", "user_id"}
        ))
@Getter
@Setter
@NoArgsConstructor
public class Reward extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "company_id", nullable = false)
    private Company company;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "campaign_id", nullable = false)
    private Campaign campaign;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "conversion_id", nullable = false)
    private Conversion conversion;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private PlatformUser user;

    @Enumerated(EnumType.STRING)
    @Column(name = "reward_type", nullable = false)
    private RewardType rewardType;

    @Column(name = "reward_value", nullable = false, precision = 10, scale = 2)
    private BigDecimal rewardValue;

    @Column(name = "coupon_code", nullable = false, unique = true, length = 100)
    private String couponCode;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RewardStatus status = RewardStatus.ISSUED;

    @Column(name = "redeemed_at")
    private LocalDateTime redeemedAt;
}
