package com.actpro.referral.user;

import com.actpro.referral.common.BaseEntity;
import com.actpro.referral.company.Company;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "platform_users",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_platform_users_company_external",
                columnNames = {"company_id", "external_user_id"}
        ))
@Getter
@Setter
@NoArgsConstructor
public class PlatformUser extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "company_id", nullable = false)
    private Company company;

    @Column(name = "external_user_id", nullable = false)
    private String externalUserId;

    @Column(nullable = false)
    private String email;

    private String name;
}
