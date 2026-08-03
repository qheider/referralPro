package com.actpro.referral.referral;

import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ReferralLinkRepository extends JpaRepository<ReferralLink, Long> {

    Optional<ReferralLink> findByPublicToken(String publicToken);

    @Query("""
            SELECT rl
            FROM ReferralLink rl
            JOIN FETCH rl.campaign c
            JOIN FETCH rl.company co
            JOIN FETCH rl.ambassadorUser au
            WHERE rl.publicToken = :publicToken
            """)
    Optional<ReferralLink> findDetailedByPublicToken(@Param("publicToken") String publicToken);

    @Modifying(clearAutomatically = true)
    @Query("UPDATE ReferralLink rl SET rl.clickCount = rl.clickCount + 1 WHERE rl.id = :id")
    void incrementClickCount(@Param("id") Long id);

    Optional<ReferralLink> findByIdAndCompanyId(Long id, Long companyId);

    Optional<ReferralLink> findByCampaignIdAndAmbassadorUserId(Long campaignId, Long ambassadorUserId);

    Optional<ReferralLink> findByCampaignIdAndAmbassadorUserIdAndCompanyId(Long campaignId, Long ambassadorUserId, Long companyId);

    List<ReferralLink> findByAmbassadorUserIdAndCompanyId(Long ambassadorUserId, Long companyId);

    List<ReferralLink> findByAmbassadorUserIdAndCompanyIdAndStatus(Long ambassadorUserId, Long companyId, ReferralLinkStatus status);

    @Query("""
            SELECT rl
            FROM ReferralLink rl
            JOIN FETCH rl.campaign c
            JOIN FETCH rl.assignment a
            WHERE rl.ambassadorUser.id = :ambassadorUserId
              AND rl.company.id = :companyId
            ORDER BY c.startDate DESC, c.name ASC
            """)
    List<ReferralLink> findDetailedByAmbassadorUserIdAndCompanyId(
            @Param("ambassadorUserId") Long ambassadorUserId,
            @Param("companyId") Long companyId
    );

    @Query("""
            SELECT rl
            FROM ReferralLink rl
            JOIN FETCH rl.campaign c
            JOIN FETCH rl.assignment a
            WHERE rl.ambassadorUser.id = :ambassadorUserId
              AND rl.company.id = :companyId
              AND rl.campaign.id = :campaignId
            """)
    Optional<ReferralLink> findDetailedByCampaignIdAndAmbassadorUserIdAndCompanyId(
            @Param("campaignId") Long campaignId,
            @Param("ambassadorUserId") Long ambassadorUserId,
            @Param("companyId") Long companyId
    );

    boolean existsByPublicToken(String publicToken);
}
