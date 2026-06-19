package com.actpro.referral.conversion;

import com.actpro.referral.campaign.Campaign;
import com.actpro.referral.common.BaseEntity;
import com.actpro.referral.company.Company;
import com.actpro.referral.referral.Referral;
import com.actpro.referral.user.PlatformUser;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "conversions",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_conversions_referral_referee",
                columnNames = {"referral_id", "referee_user_id"}
        ))
@Getter
@Setter
@NoArgsConstructor
public class Conversion extends BaseEntity {

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
    @JoinColumn(name = "referrer_user_id", nullable = false)
    private PlatformUser referrerUser;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "referee_user_id", nullable = false)
    private PlatformUser refereeUser;

    @Column(name = "event_name", nullable = false, length = 100)
    private String eventName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ConversionStatus status = ConversionStatus.PENDING;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;
}
