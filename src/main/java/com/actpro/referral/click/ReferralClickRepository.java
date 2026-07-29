package com.actpro.referral.click;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface ReferralClickRepository extends JpaRepository<ReferralClick, Long> {

    long countByAmbassadorUserIdAndCompanyId(Long ambassadorUserId, Long companyId);

    long countByAmbassadorUserIdAndCompanyIdAndCampaignId(Long ambassadorUserId, Long companyId, Long campaignId);

    List<ReferralClick> findByAmbassadorUserIdAndCompanyIdAndClickedAtBetween(
            Long ambassadorUserId,
            Long companyId,
            LocalDateTime start,
            LocalDateTime end
    );
}
