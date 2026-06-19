package com.actpro.referral.campaign;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CampaignRepository extends JpaRepository<Campaign, Long> {

    Optional<Campaign> findByIdAndCompanyId(Long id, Long companyId);

    List<Campaign> findByCompanyId(Long companyId);
}
