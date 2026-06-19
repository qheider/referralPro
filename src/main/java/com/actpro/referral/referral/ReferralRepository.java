package com.actpro.referral.referral;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ReferralRepository extends JpaRepository<Referral, Long> {

    @Query("SELECT r FROM Referral r LEFT JOIN FETCH r.campaign WHERE r.referralCode = :code")
    Optional<Referral> findByReferralCodeWithCampaign(@Param("code") String code);

    Optional<Referral> findByReferralCode(String referralCode);

    Optional<Referral> findByReferralCodeAndCompanyId(String referralCode, Long companyId);

    boolean existsByReferralCode(String referralCode);
}
