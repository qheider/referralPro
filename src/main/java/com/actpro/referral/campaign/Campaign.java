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
@Table(
        name = "campaigns",
        uniqueConstraints = @UniqueConstraint(name = "uk_campaigns_campaign_code", columnNames = "campaign_code")
)
@Getter
@Setter
@NoArgsConstructor
public class Campaign extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "company_id", nullable = false)
    private Company company;

    // Public, non-sequential identifier for the /join/{campaignCode} enrollment link - never the
    // database primary key. See CampaignCodeGenerator.
    @Column(name = "campaign_code", nullable = false, length = 20)
    private String campaignCode;

    @Column(nullable = false)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    // Captured/displayed only for now - no enforcement (e.g. budgetCap isn't checked against
    // spend anywhere yet). See CampaignService/CreateCampaignRequest.
    @Column(name = "qualifying_conditions", columnDefinition = "TEXT")
    private String qualifyingConditions;

    @Column(name = "incentive_description", columnDefinition = "TEXT")
    private String incentiveDescription;

    @Column(name = "terms_url", length = 500)
    private String termsUrl;

    @Column(name = "budget_cap", precision = 12, scale = 2)
    private BigDecimal budgetCap;

    @Column(name = "landing_page_url", nullable = false, length = 500)
    private String landingPageUrl;

    // Customer-referral window (the original, pre-Luup fields - referral clicks/conversions are
    // only meaningful while the campaign is active within this window).
    @Column(name = "start_date", nullable = false)
    private LocalDateTime startDate;

    @Column(name = "end_date", nullable = false)
    private LocalDateTime endDate;

    // Ambassador-enrollment window - may close before the referral window does (e.g. recruit
    // ambassadors only in the campaign's first month, let them refer for the full campaign).
    @Column(name = "ambassador_enrollment_start", nullable = false)
    private LocalDateTime ambassadorEnrollmentStart;

    @Column(name = "ambassador_enrollment_end", nullable = false)
    private LocalDateTime ambassadorEnrollmentEnd;

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

    public boolean isEnrollmentOpen() {
        LocalDateTime now = LocalDateTime.now();
        // SCHEDULED counts too, not just ACTIVE: enrollment recruiting ambassadors before the
        // referral period begins is the entire point of a separate enrollment window.
        return (status == CampaignStatus.SCHEDULED || status == CampaignStatus.ACTIVE) &&
                !now.isBefore(ambassadorEnrollmentStart) &&
                !now.isAfter(ambassadorEnrollmentEnd);
    }
}
