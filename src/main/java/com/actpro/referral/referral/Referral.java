package com.actpro.referral.referral;

import com.actpro.referral.auth.DashboardUser;
import com.actpro.referral.campaign.Campaign;
import com.actpro.referral.common.BaseEntity;
import com.actpro.referral.company.Company;
import com.actpro.referral.user.PlatformUser;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

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

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ambassador_user_id")
    private DashboardUser ambassadorUser;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "referral_link_id")
    private ReferralLink referralLinkEntity;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_user_id")
    private PlatformUser customerUser;

    @Column(name = "referral_code", nullable = false, unique = true, length = 50)
    private String referralCode;

    @Column(name = "referral_link", nullable = false, length = 500)
    private String referralLink;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ReferralStatus status = ReferralStatus.ACTIVE;

    @Column(name = "registered_at")
    private LocalDateTime registeredAt;

    @Column(name = "converted_at")
    private LocalDateTime convertedAt;

    @Column(name = "booking_id", length = 100)
    private String bookingId;

    @Column(name = "rental_id", length = 100)
    private String rentalId;

    @Column(name = "discount_amount", precision = 10, scale = 2)
    private BigDecimal discountAmount;

    @Column(length = 10)
    private String currency;
}
