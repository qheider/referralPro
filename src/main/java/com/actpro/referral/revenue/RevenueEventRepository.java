package com.actpro.referral.revenue;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface RevenueEventRepository extends JpaRepository<RevenueEvent, Long> {

    // Idempotency guard backed by uk_revenue_events_referral.
    Optional<RevenueEvent> findByReferralId(Long referralId);

    Optional<RevenueEvent> findByIdAndCompanyId(Long id, Long companyId);
}
