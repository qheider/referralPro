package com.actpro.referral.company;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CompanyIntegrationRepository extends JpaRepository<CompanyIntegration, Long> {

    Optional<CompanyIntegration> findByCompanyId(Long companyId);

    // Resolves the company an inbound webhook belongs to.
    Optional<CompanyIntegration> findByWebhookPublicId(String webhookPublicId);

    boolean existsByWebhookPublicId(String webhookPublicId);
}
