package com.actpro.referral.ambassador;

import com.actpro.referral.common.BaseEntity;
import com.actpro.referral.company.Company;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "ambassador_applications")
@Getter
@Setter
@NoArgsConstructor
public class AmbassadorApplication extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "company_id", nullable = false)
    private Company company;

    @Column(name = "first_name", nullable = false, length = 100)
    private String firstName;

    @Column(name = "last_name", nullable = false, length = 100)
    private String lastName;

    // Stored lowercase-normalized (see AmbassadorApplicationService.submitApplication) so
    // existsByCompanyIdAndEmailAndStatus lookups and the DashboardUser.username uniqueness
    // check compare like-for-like.
    @Column(nullable = false, length = 100)
    private String email;

    @Column(length = 50)
    private String phone;

    @Column(name = "display_name", length = 255)
    private String displayName;

    @Column(columnDefinition = "TEXT")
    private String bio;

    @Column(name = "social_media_platform", length = 100)
    private String socialMediaPlatform;

    @Column(name = "social_media_handle", length = 255)
    private String socialMediaHandle;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ApplicationStatus status = ApplicationStatus.PENDING;

    @Column(name = "rejection_reason", length = 500)
    private String rejectionReason;

    // Plain FK column rather than a @ManyToOne DashboardUser - this is an audit-only reference
    // (who reviewed this application), not a relationship the app needs to traverse/lazy-load.
    @Column(name = "reviewed_by_user_id")
    private Long reviewedByUserId;

    @Column(name = "reviewed_at")
    private LocalDateTime reviewedAt;

    // Set on approval; traceability link to the AmbassadorProfile this application produced.
    // Same reasoning as reviewedByUserId - plain column, no JPA relation needed.
    @Column(name = "resulting_ambassador_profile_id")
    private Long resultingAmbassadorProfileId;
}
