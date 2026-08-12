package com.actpro.referral.referral;

import com.actpro.referral.ambassador.CampaignAmbassadorAssignment;
import com.actpro.referral.auth.DashboardUser;
import com.actpro.referral.campaign.Campaign;
import com.actpro.referral.common.BaseEntity;
import com.actpro.referral.company.Company;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "referral_links",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_referral_links_public_token", columnNames = "public_token"),
                @UniqueConstraint(name = "uk_referral_links_campaign_ambassador", columnNames = {"campaign_id", "ambassador_user_id"})
        }
)
@Getter
@Setter
@NoArgsConstructor
public class ReferralLink extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "company_id", nullable = false)
    private Company company;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "campaign_id", nullable = false)
    private Campaign campaign;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ambassador_user_id", nullable = false)
    private DashboardUser ambassadorUser;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assignment_id", nullable = false)
    private CampaignAmbassadorAssignment assignment;

    @Column(name = "public_token", nullable = false, length = 64)
    private String publicToken;

    @Column(name = "destination_url", length = 500)
    private String destinationUrl;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private ReferralLinkStatus status = ReferralLinkStatus.ACTIVE;

    @Column(name = "click_count", nullable = false)
    private Long clickCount = 0L;

    @Column(name = "expires_at")
    private LocalDateTime expiresAt;
}
