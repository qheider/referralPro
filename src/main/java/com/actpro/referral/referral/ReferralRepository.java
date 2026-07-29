package com.actpro.referral.referral;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface ReferralRepository extends JpaRepository<Referral, Long> {

    @Query("SELECT r FROM Referral r LEFT JOIN FETCH r.campaign WHERE r.referralCode = :code")
    Optional<Referral> findByReferralCodeWithCampaign(@Param("code") String code);

    Optional<Referral> findByReferralCode(String referralCode);

    Optional<Referral> findByReferralCodeAndCompanyId(String referralCode, Long companyId);

    boolean existsByReferralCode(String referralCode);

    long countByAmbassadorUserIdAndCompanyId(Long ambassadorUserId, Long companyId);

    long countByAmbassadorUserIdAndCompanyIdAndStatusIn(
            Long ambassadorUserId,
            Long companyId,
            Collection<ReferralStatus> statuses
    );

    long countByAmbassadorUserIdAndCompanyIdAndStatus(Long ambassadorUserId, Long companyId, ReferralStatus status);

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
}
