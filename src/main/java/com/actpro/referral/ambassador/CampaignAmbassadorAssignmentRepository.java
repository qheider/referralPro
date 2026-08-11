package com.actpro.referral.ambassador;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface CampaignAmbassadorAssignmentRepository extends JpaRepository<CampaignAmbassadorAssignment, Long> {

    Optional<CampaignAmbassadorAssignment> findByCampaignIdAndAmbassadorUserIdAndCompanyId(
            Long campaignId,
            Long ambassadorUserId,
            Long companyId
    );

    Optional<CampaignAmbassadorAssignment> findByCampaignIdAndAmbassadorUserIdAndCompanyIdAndStatus(
            Long campaignId,
            Long ambassadorUserId,
            Long companyId,
            AssignmentStatus status
    );

    Optional<CampaignAmbassadorAssignment> findByIdAndCompanyId(Long id, Long companyId);

    List<CampaignAmbassadorAssignment> findByCampaignIdAndCompanyIdAndStatus(Long campaignId, Long companyId, AssignmentStatus status);

    List<CampaignAmbassadorAssignment> findByAmbassadorUserIdAndCompanyIdAndStatus(Long ambassadorUserId, Long companyId, AssignmentStatus status);

    long countByAmbassadorUserIdAndCompanyIdAndStatus(Long ambassadorUserId, Long companyId, AssignmentStatus status);

    // Batched form of countByAmbassadorUserIdAndCompanyIdAndStatus for a page of ambassadors -
    // one grouped query instead of one count query per ambassador. See
    // AmbassadorAdminService.listAmbassadors.
    @Query("""
            SELECT caa.ambassadorUser.id, COUNT(caa)
            FROM CampaignAmbassadorAssignment caa
            WHERE caa.company.id = :companyId
              AND caa.ambassadorUser.id IN :ambassadorUserIds
              AND caa.status = :status
            GROUP BY caa.ambassadorUser.id
            """)
    List<Object[]> countByAmbassadorUserIdsAndCompanyIdAndStatusGrouped(
            @Param("ambassadorUserIds") Collection<Long> ambassadorUserIds,
            @Param("companyId") Long companyId,
            @Param("status") AssignmentStatus status
    );

    @Query("""
            SELECT caa
            FROM CampaignAmbassadorAssignment caa
            JOIN FETCH caa.ambassadorUser ambassador
            JOIN FETCH caa.campaign campaign
            WHERE caa.campaign.id = :campaignId
              AND caa.company.id = :companyId
              AND caa.status = :status
            ORDER BY caa.assignedAt DESC
            """)
    List<CampaignAmbassadorAssignment> findDetailedByCampaignAndStatus(
            @Param("campaignId") Long campaignId,
            @Param("companyId") Long companyId,
            @Param("status") AssignmentStatus status
    );

    @Query("""
            SELECT caa
            FROM CampaignAmbassadorAssignment caa
            JOIN FETCH caa.campaign campaign
            JOIN FETCH caa.ambassadorUser ambassador
            WHERE caa.ambassadorUser.id = :ambassadorUserId
              AND caa.company.id = :companyId
              AND caa.status = :status
            ORDER BY caa.assignedAt DESC
            """)
    List<CampaignAmbassadorAssignment> findDetailedByAmbassadorAndStatus(
            @Param("ambassadorUserId") Long ambassadorUserId,
            @Param("companyId") Long companyId,
            @Param("status") AssignmentStatus status
    );

    @Query("""
            SELECT caa
            FROM CampaignAmbassadorAssignment caa
            JOIN FETCH caa.campaign campaign
            JOIN FETCH caa.ambassadorUser ambassador
            WHERE caa.campaign.id = :campaignId
              AND caa.ambassadorUser.id = :ambassadorUserId
              AND caa.company.id = :companyId
              AND caa.status = :status
            """)
    Optional<CampaignAmbassadorAssignment> findDetailedByCampaignAndAmbassadorAndStatus(
            @Param("campaignId") Long campaignId,
            @Param("ambassadorUserId") Long ambassadorUserId,
            @Param("companyId") Long companyId,
            @Param("status") AssignmentStatus status
    );
}
