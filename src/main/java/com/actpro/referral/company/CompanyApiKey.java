package com.actpro.referral.company;

import com.actpro.referral.common.BaseEntity;
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
        name = "company_api_keys",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_company_api_keys_key_id", columnNames = "key_id"),
                @UniqueConstraint(name = "uk_company_api_keys_secret_hash", columnNames = "secret_hash")
        }
)
@Getter
@Setter
@NoArgsConstructor
public class CompanyApiKey extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "company_id", nullable = false)
    private Company company;

    // Public, non-secret identifier for referencing this key in the UI/logs - never the secret itself.
    @Column(name = "key_id", nullable = false, length = 40)
    private String keyId;

    // SHA-256 hex digest of the full raw key. Deterministic (unsalted) hashing is safe here because
    // the raw key is a high-entropy generated secret, not a user-chosen password, and it lets the
    // auth filter do a single indexed equality lookup instead of scanning candidate rows.
    @Column(name = "secret_hash", nullable = false, length = 64)
    private String secretHash;

    // Last few characters of the raw key, retained only for the company admin to visually
    // distinguish keys in a list - not enough entropy to reconstruct the secret.
    @Column(name = "secret_preview", nullable = false, length = 12)
    private String secretPreview;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private CompanyApiKeyStatus status = CompanyApiKeyStatus.ACTIVE;

    @Column(name = "expires_at")
    private LocalDateTime expiresAt;

    @Column(name = "last_used_at")
    private LocalDateTime lastUsedAt;

    @Column(name = "revoked_at")
    private LocalDateTime revokedAt;

    @Column(name = "rotated_from_key_id")
    private Long rotatedFromKeyId;
}
