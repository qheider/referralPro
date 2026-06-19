package com.actpro.referral.referral;

import com.actpro.referral.campaign.Campaign;
import com.actpro.referral.common.BaseEntity;
import com.actpro.referral.company.Company;
import com.actpro.referral.user.PlatformUser;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "referrals")
@Getter
@Setter
@NoArgsConstructor
public class Referral extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "company_id", nullable = false)
    private Company company;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "campaign_id", nullable = false)
    private Campaign campaign;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "referrer_user_id", nullable = false)
    private PlatformUser referrerUser;

    @Column(name = "referral_code", nullable = false, unique = true, length = 50)
    private String referralCode;

    @Column(name = "referral_link", nullable = false, length = 500)
    private String referralLink;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ReferralStatus status = ReferralStatus.ACTIVE;
}
