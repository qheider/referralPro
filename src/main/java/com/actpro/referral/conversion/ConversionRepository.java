package com.actpro.referral.conversion;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ConversionRepository extends JpaRepository<Conversion, Long> {

    boolean existsByReferralIdAndRefereeUserId(Long referralId, Long refereeUserId);
}
