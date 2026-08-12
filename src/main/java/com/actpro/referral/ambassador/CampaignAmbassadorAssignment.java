package com.actpro.referral.ambassador;

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
        name = "campaign_ambassador_assignments",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_campaign_ambassador_assignments_campaign_user",
                columnNames = {"campaign_id", "ambassador_user_id"}
        )
)
@Getter
@Setter
@NoArgsConstructor
public class CampaignAmbassadorAssignment extends BaseEntity {

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
    @JoinColumn(name = "assigned_by_user_id")
    private DashboardUser assignedBy;

    @Column(name = "assigned_at", nullable = false)
    private LocalDateTime assignedAt = LocalDateTime.now();

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private AssignmentStatus status = AssignmentStatus.ACTIVE;
}
