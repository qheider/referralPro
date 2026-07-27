package com.actpro.referral.ambassador;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CampaignAmbassadorAssignmentRepository extends JpaRepository<CampaignAmbassadorAssignment, Long> {

    Optional<CampaignAmbassadorAssignment> findByCampaignIdAndAmbassadorUserId(Long campaignId, Long ambassadorUserId);

    Optional<CampaignAmbassadorAssignment> findByIdAndCompanyId(Long id, Long companyId);

    List<CampaignAmbassadorAssignment> findByCampaignIdAndStatus(Long campaignId, AssignmentStatus status);

    List<CampaignAmbassadorAssignment> findByCampaignIdAndCompanyIdAndStatus(Long campaignId, Long companyId, AssignmentStatus status);

    List<CampaignAmbassadorAssignment> findByAmbassadorUserIdAndStatus(Long ambassadorUserId, AssignmentStatus status);

    List<CampaignAmbassadorAssignment> findByAmbassadorUserIdAndCompanyIdAndStatus(Long ambassadorUserId, Long companyId, AssignmentStatus status);

    long countByAmbassadorUserIdAndCompanyIdAndStatus(Long ambassadorUserId, Long companyId, AssignmentStatus status);
}
