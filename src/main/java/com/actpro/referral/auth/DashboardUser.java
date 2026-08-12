package com.actpro.referral.auth;

import com.actpro.referral.common.BaseEntity;
import com.actpro.referral.company.Company;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "dashboard_users")
@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@AllArgsConstructor
public class DashboardUser extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "company_id", nullable = false)
    private Company company;

    @Column(nullable = false, unique = true, length = 100)
    private String username;

    @Column(nullable = false)
    private String password;

    @Column(name = "first_name", length = 100)
    private String firstName;

    @Column(name = "last_name", length = 100)
    private String lastName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private UserRole role = UserRole.COMPANY_ADMIN;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private UserStatus status = UserStatus.ACTIVE;

    @Column(name = "last_login_at")
    private LocalDateTime lastLoginAt;
}
