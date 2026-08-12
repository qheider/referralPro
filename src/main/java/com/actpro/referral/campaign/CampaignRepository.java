package com.actpro.referral.campaign;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface CampaignRepository extends JpaRepository<Campaign, Long> {

    Optional<Campaign> findByIdAndCompanyId(Long id, Long companyId);

    List<Campaign> findByCompanyId(Long companyId);

    boolean existsByCampaignCode(String campaignCode);

    Optional<Campaign> findByCampaignCode(String campaignCode);

    // Due for the expiration worker's SCHEDULED -> ACTIVE transition.
    List<Campaign> findByStatusAndStartDateLessThanEqual(CampaignStatus status, LocalDateTime now);

    // Due for the expiration worker's ACTIVE/PAUSED -> EXPIRED transition.
    List<Campaign> findByStatusInAndEndDateLessThanEqual(List<CampaignStatus> statuses, LocalDateTime now);
}
