package com.actpro.referral.auth;

import com.actpro.referral.common.BaseEntity;
import com.actpro.referral.company.Company;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "account_invitations",
        uniqueConstraints = @UniqueConstraint(name = "uk_account_invitations_token_hash", columnNames = "token_hash")
)
@Getter
@Setter
@NoArgsConstructor
public class AccountInvitation extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "dashboard_user_id", nullable = false)
    private DashboardUser dashboardUser;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "company_id", nullable = false)
    private Company company;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private InvitationPurpose purpose;

    // SHA-256 hex digest of the raw token - same rationale as CompanyApiKey.secretHash: the raw
    // token is a high-entropy generated secret, so deterministic hashing supports an indexed
    // equality lookup without the salting a user-chosen password would need.
    @Column(name = "token_hash", nullable = false, length = 64)
    private String tokenHash;

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    @Column(name = "accepted_at")
    private LocalDateTime acceptedAt;

    @Column(name = "revoked_at")
    private LocalDateTime revokedAt;

    public boolean isUsable() {
        return acceptedAt == null && revokedAt == null && expiresAt.isAfter(LocalDateTime.now());
    }
}
