package com.actpro.referral.campaign;

import com.actpro.referral.common.BaseEntity;
import com.actpro.referral.company.Company;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "campaigns")
@Getter
@Setter
@NoArgsConstructor
public class Campaign extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "company_id", nullable = false)
    private Company company;

    @Column(nullable = false)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "landing_page_url", nullable = false, length = 500)
    private String landingPageUrl;

    @Column(name = "start_date", nullable = false)
    private LocalDateTime startDate;

    @Column(name = "end_date", nullable = false)
    private LocalDateTime endDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "reward_type", nullable = false)
    private RewardType rewardType;

    @Column(name = "referrer_reward_value", nullable = false, precision = 10, scale = 2)
    private BigDecimal referrerRewardValue;

    @Column(name = "referee_reward_value", nullable = false, precision = 10, scale = 2)
    private BigDecimal refereeRewardValue;

    @Column(name = "conversion_event_name", nullable = false, length = 100)
    private String conversionEventName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CampaignStatus status = CampaignStatus.DRAFT;

    public boolean isActive() {
        return status == CampaignStatus.ACTIVE &&
                LocalDateTime.now().isAfter(startDate) &&
                LocalDateTime.now().isBefore(endDate);
    }
}
