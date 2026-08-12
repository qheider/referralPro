package com.actpro.referral.referral;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface ReferralRepository extends JpaRepository<Referral, Long> {

    @Query("SELECT r FROM Referral r LEFT JOIN FETCH r.campaign WHERE r.referralCode = :code")
    Optional<Referral> findByReferralCodeWithCampaign(@Param("code") String code);

    Optional<Referral> findByReferralCodeAndCompanyId(String referralCode, Long companyId);

    boolean existsByReferralCode(String referralCode);

    long countByAmbassadorUserIdAndCompanyId(Long ambassadorUserId, Long companyId);

    long countByAmbassadorUserIdAndCompanyIdAndStatusIn(
            Long ambassadorUserId,
            Long companyId,
            Collection<ReferralStatus> statuses
    );

    long countByAmbassadorUserIdAndCompanyIdAndStatus(Long ambassadorUserId, Long companyId, ReferralStatus status);

    // Batched forms of the two counts above for a page of ambassadors - one grouped query each
    // instead of one count query per ambassador per stat. See AmbassadorAdminService.listAmbassadors.
    @Query("""
            SELECT r.ambassadorUser.id, COUNT(r)
            FROM Referral r
            WHERE r.company.id = :companyId
              AND r.ambassadorUser.id IN :ambassadorUserIds
              AND r.status IN :statuses
            GROUP BY r.ambassadorUser.id
            """)
    List<Object[]> countByAmbassadorUserIdsAndCompanyIdAndStatusInGrouped(
            @Param("ambassadorUserIds") Collection<Long> ambassadorUserIds,
            @Param("companyId") Long companyId,
            @Param("statuses") Collection<ReferralStatus> statuses
    );

    @Query("""
            SELECT r.ambassadorUser.id, COUNT(r)
            FROM Referral r
            WHERE r.company.id = :companyId
              AND r.ambassadorUser.id IN :ambassadorUserIds
              AND r.status = :status
            GROUP BY r.ambassadorUser.id
            """)
    List<Object[]> countByAmbassadorUserIdsAndCompanyIdAndStatusGrouped(
            @Param("ambassadorUserIds") Collection<Long> ambassadorUserIds,
            @Param("companyId") Long companyId,
            @Param("status") ReferralStatus status
    );

    long countByAmbassadorUserIdAndCompanyIdAndCampaignIdAndStatusIn(
            Long ambassadorUserId,
            Long companyId,
            Long campaignId,
            Collection<ReferralStatus> statuses
    );

    long countByAmbassadorUserIdAndCompanyIdAndCampaignIdAndStatus(
            Long ambassadorUserId,
            Long companyId,
            Long campaignId,
            ReferralStatus status
    );

    @Query(
            value = """
                    SELECT r
                    FROM Referral r
                    JOIN FETCH r.campaign c
                    LEFT JOIN FETCH r.customerUser customer
                    LEFT JOIN FETCH r.referrerUser referrer
                    WHERE r.ambassadorUser.id = :ambassadorUserId
                      AND r.company.id = :companyId
                      AND (:campaignId IS NULL OR c.id = :campaignId)
                      AND (:status IS NULL OR r.status = :status)
                      AND (:fromDate IS NULL OR r.createdAt >= :fromDate)
                      AND (:toDate IS NULL OR r.createdAt <= :toDate)
                    """,
            countQuery = """
                    SELECT COUNT(r)
                    FROM Referral r
                    JOIN r.campaign c
                    WHERE r.ambassadorUser.id = :ambassadorUserId
                      AND r.company.id = :companyId
                      AND (:campaignId IS NULL OR c.id = :campaignId)
                      AND (:status IS NULL OR r.status = :status)
                      AND (:fromDate IS NULL OR r.createdAt >= :fromDate)
                      AND (:toDate IS NULL OR r.createdAt <= :toDate)
                    """
    )
    Page<Referral> searchByAmbassador(
            @Param("ambassadorUserId") Long ambassadorUserId,
            @Param("companyId") Long companyId,
            @Param("campaignId") Long campaignId,
            @Param("status") ReferralStatus status,
            @Param("fromDate") LocalDateTime fromDate,
            @Param("toDate") LocalDateTime toDate,
            Pageable pageable
    );

    @Query("""
            SELECT r
            FROM Referral r
            JOIN FETCH r.campaign c
            LEFT JOIN FETCH r.customerUser customer
            LEFT JOIN FETCH r.referrerUser referrer
            WHERE r.ambassadorUser.id = :ambassadorUserId
              AND r.company.id = :companyId
            ORDER BY COALESCE(r.registeredAt, r.createdAt) DESC
            """)
    java.util.List<Referral> findRecentByAmbassador(
            @Param("ambassadorUserId") Long ambassadorUserId,
            @Param("companyId") Long companyId,
            Pageable pageable
    );

    java.util.List<Referral> findByAmbassadorUserIdAndCompanyIdAndCreatedAtBetween(
            Long ambassadorUserId,
            Long companyId,
            LocalDateTime start,
            LocalDateTime end
    );

    // Same-session idempotent lookup: a resubmitted lead form in the same browsing session should
    // return the referral already created for it, not create a duplicate.
    Optional<Referral> findByReferralLinkEntityIdAndAttributionSessionId(Long referralLinkEntityId, String attributionSessionId);

    // Cross-session/cross-device duplicate check: is there already a non-terminal referral for
    // this email against this link? Terminal statuses are excluded so a prior dead-end submission
    // doesn't block a legitimate resubmission.
    boolean existsByReferralLinkEntityIdAndCustomerUserEmailAndStatusNotIn(
            Long referralLinkEntityId,
            String email,
            Collection<ReferralStatus> excludedStatuses
    );
}
