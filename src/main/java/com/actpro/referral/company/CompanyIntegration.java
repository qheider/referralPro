package com.actpro.referral.company;

import com.actpro.referral.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Minimal stub seeded (status NOT_CONFIGURED) at company registration time. The full config
 * surface - API URL, auth type, encrypted credentials/OAuth, timeout, retry policy, field
 * mapping, webhook signing, status mapping, last test result - is Phase 6 scope and will extend
 * this entity/table rather than reshape it.
 */
@Entity
@Table(
        name = "company_integrations",
        uniqueConstraints = @UniqueConstraint(name = "uk_company_integrations_company_id", columnNames = "company_id")
)
@Getter
@Setter
@NoArgsConstructor
public class CompanyIntegration extends BaseEntity {

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "company_id", nullable = false)
    private Company company;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private CompanyIntegrationStatus status = CompanyIntegrationStatus.NOT_CONFIGURED;
}
