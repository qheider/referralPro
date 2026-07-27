package com.actpro.referral.ambassador;

import com.actpro.referral.auth.DashboardUser;
import com.actpro.referral.common.BaseEntity;
import com.actpro.referral.company.Company;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "ambassador_profiles",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_ambassador_profiles_user_id", columnNames = "user_id"),
                @UniqueConstraint(name = "uk_ambassador_profiles_code", columnNames = "ambassador_code")
        }
)
@Getter
@Setter
@NoArgsConstructor
public class AmbassadorProfile extends BaseEntity {

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private DashboardUser user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "company_id", nullable = false)
    private Company company;

    @Column(name = "display_name", length = 255)
    private String displayName;

    @Column(length = 50)
    private String phone;

    @Column(columnDefinition = "TEXT")
    private String bio;

    @Column(name = "social_media_handle", length = 255)
    private String socialMediaHandle;

    @Column(name = "social_media_platform", length = 100)
    private String socialMediaPlatform;

    @Column(name = "profile_image_url", length = 500)
    private String profileImageUrl;

    @Column(name = "ambassador_code", nullable = false, length = 50)
    private String ambassadorCode;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private AmbassadorStatus status = AmbassadorStatus.INVITED;

    @Column(name = "joined_at")
    private LocalDateTime joinedAt;
}
