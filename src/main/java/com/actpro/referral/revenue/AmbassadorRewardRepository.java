package com.actpro.referral.revenue;

import org.springframework.data.domain.Limit;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AmbassadorRewardRepository extends JpaRepository<AmbassadorReward, Long> {

    // Idempotency guard backed by uk_ambassador_rewards_revenue_event.
    Optional<AmbassadorReward> findByRevenueEventId(Long revenueEventId);

    Optional<AmbassadorReward> findByIdAndCompanyId(Long id, Long companyId);

    // Ambassador's own earnings history (paginated) and full-set (unpaginated, for the earnings
    // summary aggregation - AmbassadorPortalService's existing convention of loading a bounded
    // per-ambassador list and aggregating in Java, not native SQL - see its getDashboard/getAnalytics).
    Page<AmbassadorReward> findByAmbassadorUserIdAndCompanyId(Long ambassadorUserId, Long companyId, Pageable pageable);

    List<AmbassadorReward> findByAmbassadorUserIdAndCompanyId(Long ambassadorUserId, Long companyId);

    // Admin monitoring listing, same shape as ApiSubmissionRepository's Limit-based finders.
    List<AmbassadorReward> findByCompanyIdOrderByCreatedAtDesc(Long companyId, Limit limit);

    List<AmbassadorReward> findByCompanyIdAndStatusOrderByCreatedAtDesc(Long companyId, AmbassadorRewardStatus status, Limit limit);

    List<AmbassadorReward> findByCompanyIdAndCampaignIdOrderByCreatedAtDesc(Long companyId, Long campaignId, Limit limit);

    List<AmbassadorReward> findByCompanyIdAndCampaignIdAndStatusOrderByCreatedAtDesc(
            Long companyId, Long campaignId, AmbassadorRewardStatus status, Limit limit);
}
