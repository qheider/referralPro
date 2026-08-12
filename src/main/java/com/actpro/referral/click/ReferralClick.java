package com.actpro.referral.click;

import com.actpro.referral.auth.DashboardUser;
import com.actpro.referral.campaign.Campaign;
import com.actpro.referral.company.Company;
import com.actpro.referral.referral.Referral;
import com.actpro.referral.referral.ReferralLink;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "referral_clicks")
@Getter
@Setter
@NoArgsConstructor
public class ReferralClick {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "referral_id")
    private Referral referral;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "referral_link_id")
    private ReferralLink referralLink;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "company_id")
    private Company company;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "campaign_id")
    private Campaign campaign;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ambassador_user_id")
    private DashboardUser ambassadorUser;

    @Column(name = "session_id", length = 100)
    private String sessionId;

    @Column(name = "ip_address", length = 50)
    private String ipAddress;

    @Column(name = "ip_hash", length = 255)
    private String ipHash;

    @Column(name = "user_agent", length = 500)
    private String userAgent;

    @Column(name = "referrer_url", length = 1000)
    private String referrerUrl;

    @Column(name = "clicked_at", nullable = false)
    private LocalDateTime clickedAt = LocalDateTime.now();
}
