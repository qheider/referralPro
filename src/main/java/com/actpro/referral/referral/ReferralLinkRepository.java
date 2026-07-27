package com.actpro.referral.referral;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ReferralLinkRepository extends JpaRepository<ReferralLink, Long> {

    Optional<ReferralLink> findByPublicToken(String publicToken);

    Optional<ReferralLink> findByIdAndCompanyId(Long id, Long companyId);

    Optional<ReferralLink> findByCampaignIdAndAmbassadorUserId(Long campaignId, Long ambassadorUserId);

    Optional<ReferralLink> findByCampaignIdAndAmbassadorUserIdAndCompanyId(Long campaignId, Long ambassadorUserId, Long companyId);

    List<ReferralLink> findByAmbassadorUserIdAndCompanyId(Long ambassadorUserId, Long companyId);

    List<ReferralLink> findByAmbassadorUserIdAndCompanyIdAndStatus(Long ambassadorUserId, Long companyId, ReferralLinkStatus status);

    boolean existsByPublicToken(String publicToken);
}
