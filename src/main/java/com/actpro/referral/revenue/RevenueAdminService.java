package com.actpro.referral.revenue;

import com.actpro.referral.auth.DashboardUser;
import com.actpro.referral.campaign.Campaign;
import com.actpro.referral.campaign.CampaignRepository;
import com.actpro.referral.common.exception.BadRequestException;
import com.actpro.referral.common.exception.NotFoundException;
import com.actpro.referral.revenue.dto.AmbassadorRevenueSummaryResponse;
import com.actpro.referral.revenue.dto.AmbassadorRewardResponse;
import com.actpro.referral.revenue.dto.CampaignRevenueReportResponse;
import com.actpro.referral.security.CurrentUserService;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Limit;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Company-admin surface for Phase 8: read-only reward monitoring/reporting (native SQL for the
 * cross-entity campaign report, per CLAUDE.md's dashboard-analytics convention) plus the manual
 * ELIGIBLE -> APPROVED -> PAID / REJECTED lifecycle actions on {@link AmbassadorReward}.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RevenueAdminService {

    private final AmbassadorRewardRepository ambassadorRewardRepository;
    private final CampaignRepository campaignRepository;
    private final CurrentUserService currentUserService;
    private final EntityManager entityManager;

    @Transactional(readOnly = true)
    public List<AmbassadorRewardResponse> listRewards(Long campaignId, AmbassadorRewardStatus status, int limit) {
        Long companyId = currentUserService.getCurrentCompanyId();
        Limit limitValue = Limit.of(limit);
        List<AmbassadorReward> rewards;
        if (campaignId != null && status != null) {
            rewards = ambassadorRewardRepository.findByCompanyIdAndCampaignIdAndStatusOrderByCreatedAtDesc(companyId, campaignId, status, limitValue);
        } else if (campaignId != null) {
            rewards = ambassadorRewardRepository.findByCompanyIdAndCampaignIdOrderByCreatedAtDesc(companyId, campaignId, limitValue);
        } else if (status != null) {
            rewards = ambassadorRewardRepository.findByCompanyIdAndStatusOrderByCreatedAtDesc(companyId, status, limitValue);
        } else {
            rewards = ambassadorRewardRepository.findByCompanyIdOrderByCreatedAtDesc(companyId, limitValue);
        }
        return rewards.stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public AmbassadorRewardResponse getReward(Long rewardId) {
        return toResponse(requireReward(rewardId));
    }

    @Transactional
    public AmbassadorRewardResponse approve(Long rewardId) {
        AmbassadorReward reward = requireReward(rewardId);
        if (reward.getStatus() != AmbassadorRewardStatus.ELIGIBLE) {
            throw new BadRequestException("Only an ELIGIBLE reward can be approved (current status: " + reward.getStatus() + ")");
        }
        reward.setStatus(AmbassadorRewardStatus.APPROVED);
        reward.setApprovedAt(LocalDateTime.now());
        AmbassadorReward saved = ambassadorRewardRepository.save(reward);
        // Phase 9 hardening: basic operational traceability for a money-affecting admin action -
        // the first real caller of CurrentUserService#getCurrentActor outside its own class,
        // exactly the "who did this" use case its Javadoc names. Not a full audit trail (that's
        // an explicitly-deferred, separate feature per phases_tracker.txt's carried-forward notes).
        log.info("Reward {} approved by {} (userId={})", rewardId, currentUserService.getCurrentActor().username(), currentUserService.getCurrentUserId());
        return toResponse(saved);
    }

    @Transactional
    public AmbassadorRewardResponse markPaid(Long rewardId) {
        AmbassadorReward reward = requireReward(rewardId);
        if (reward.getStatus() != AmbassadorRewardStatus.APPROVED) {
            throw new BadRequestException("Only an APPROVED reward can be marked paid (current status: " + reward.getStatus() + ")");
        }
        reward.setStatus(AmbassadorRewardStatus.PAID);
        reward.setPaidAt(LocalDateTime.now());
        AmbassadorReward saved = ambassadorRewardRepository.save(reward);
        log.info("Reward {} marked PAID by {} (userId={}, value={} {})",
                rewardId, currentUserService.getCurrentActor().username(), currentUserService.getCurrentUserId(), reward.getRewardValue(), reward.getCurrency());
        return toResponse(saved);
    }

    @Transactional
    public AmbassadorRewardResponse reject(Long rewardId, String reason) {
        AmbassadorReward reward = requireReward(rewardId);
        if (reward.getStatus() == AmbassadorRewardStatus.PAID
                || reward.getStatus() == AmbassadorRewardStatus.REJECTED
                || reward.getStatus() == AmbassadorRewardStatus.REVERSED) {
            throw new BadRequestException("A " + reward.getStatus() + " reward cannot be rejected");
        }
        reward.setStatus(AmbassadorRewardStatus.REJECTED);
        reward.setRejectedAt(LocalDateTime.now());
        reward.setRejectionReason(reason);
        log.info("Reward {} rejected by {} (userId={}): {}", rewardId, currentUserService.getCurrentActor().username(), currentUserService.getCurrentUserId(), reason);
        return toResponse(ambassadorRewardRepository.save(reward));
    }

    @Transactional(readOnly = true)
    public CampaignRevenueReportResponse getCampaignReport(Long campaignId) {
        Long companyId = currentUserService.getCurrentCompanyId();
        Campaign campaign = campaignRepository.findByIdAndCompanyId(campaignId, companyId)
                .orElseThrow(() -> new NotFoundException("Campaign not found"));

        Object[] eventCounts = (Object[]) entityManager.createNativeQuery("""
                SELECT
                    COALESCE(SUM(CASE WHEN status = 'RECORDED' THEN 1 ELSE 0 END), 0),
                    COALESCE(SUM(CASE WHEN status = 'REVERSED' THEN 1 ELSE 0 END), 0),
                    COALESCE(SUM(CASE WHEN currency_mismatch = TRUE THEN 1 ELSE 0 END), 0)
                FROM revenue_events
                WHERE campaign_id = :campaignId AND company_id = :companyId
                """)
                .setParameter("campaignId", campaignId)
                .setParameter("companyId", companyId)
                .getSingleResult();
        long qualifyingCount = ((Number) eventCounts[0]).longValue();
        long reversedCount = ((Number) eventCounts[1]).longValue();
        long mismatchCount = ((Number) eventCounts[2]).longValue();

        Map<String, BigDecimal> revenueByCurrency = new LinkedHashMap<>();
        List<Object[]> currencyRows = entityManager.createNativeQuery("""
                SELECT currency, SUM(revenue_amount)
                FROM revenue_events
                WHERE campaign_id = :campaignId AND company_id = :companyId
                  AND status = 'RECORDED' AND currency_mismatch = FALSE
                  AND currency IS NOT NULL AND revenue_amount IS NOT NULL
                GROUP BY currency
                """)
                .setParameter("campaignId", campaignId)
                .setParameter("companyId", companyId)
                .getResultList();
        for (Object[] row : currencyRows) {
            revenueByCurrency.put((String) row[0], new BigDecimal(row[1].toString()));
        }

        Map<String, BigDecimal> valueByStatus = new LinkedHashMap<>();
        Map<String, Long> countByStatus = new LinkedHashMap<>();
        List<Object[]> rewardRows = entityManager.createNativeQuery("""
                SELECT status, COUNT(*), COALESCE(SUM(reward_value), 0)
                FROM ambassador_rewards
                WHERE campaign_id = :campaignId AND company_id = :companyId
                GROUP BY status
                """)
                .setParameter("campaignId", campaignId)
                .setParameter("companyId", companyId)
                .getResultList();
        long rewardCount = 0;
        for (Object[] row : rewardRows) {
            String status = (String) row[0];
            long count = ((Number) row[1]).longValue();
            valueByStatus.put(status, new BigDecimal(row[2].toString()));
            countByStatus.put(status, count);
            rewardCount += count;
        }

        List<Object[]> leaderboardRows = entityManager.createNativeQuery("""
                SELECT
                    ar.ambassador_user_id,
                    COALESCE(NULLIF(TRIM(CONCAT(COALESCE(du.first_name, ''), ' ', COALESCE(du.last_name, ''))), ''), du.username),
                    COALESCE(SUM(CASE WHEN rv.status = 'RECORDED' THEN 1 ELSE 0 END), 0),
                    COALESCE(SUM(CASE WHEN rv.status = 'REVERSED' THEN 1 ELSE 0 END), 0),
                    COALESCE(SUM(CASE WHEN ar.status IN ('PENDING', 'ELIGIBLE') THEN ar.reward_value ELSE 0 END), 0),
                    COALESCE(SUM(CASE WHEN ar.status = 'APPROVED' THEN ar.reward_value ELSE 0 END), 0),
                    COALESCE(SUM(CASE WHEN ar.status = 'PAID' THEN ar.reward_value ELSE 0 END), 0)
                FROM ambassador_rewards ar
                JOIN revenue_events rv ON rv.id = ar.revenue_event_id
                JOIN dashboard_users du ON du.id = ar.ambassador_user_id
                WHERE ar.campaign_id = :campaignId AND ar.company_id = :companyId
                GROUP BY ar.ambassador_user_id, du.first_name, du.last_name, du.username
                ORDER BY SUM(CASE WHEN ar.status = 'PAID' THEN ar.reward_value ELSE 0 END) DESC
                """)
                .setParameter("campaignId", campaignId)
                .setParameter("companyId", companyId)
                .getResultList();
        List<AmbassadorRevenueSummaryResponse> leaderboard = leaderboardRows.stream()
                .map(row -> new AmbassadorRevenueSummaryResponse(
                        ((Number) row[0]).longValue(),
                        (String) row[1],
                        ((Number) row[2]).longValue(),
                        ((Number) row[3]).longValue(),
                        new BigDecimal(row[4].toString()),
                        new BigDecimal(row[5].toString()),
                        new BigDecimal(row[6].toString())
                ))
                .toList();

        return new CampaignRevenueReportResponse(
                campaign.getId(),
                campaign.getName(),
                qualifyingCount,
                reversedCount,
                mismatchCount,
                revenueByCurrency,
                rewardCount,
                valueByStatus.getOrDefault("PENDING", BigDecimal.ZERO),
                valueByStatus.getOrDefault("ELIGIBLE", BigDecimal.ZERO),
                valueByStatus.getOrDefault("APPROVED", BigDecimal.ZERO),
                valueByStatus.getOrDefault("PAID", BigDecimal.ZERO),
                valueByStatus.getOrDefault("REJECTED", BigDecimal.ZERO),
                valueByStatus.getOrDefault("REVERSED", BigDecimal.ZERO),
                leaderboard
        );
    }

    private AmbassadorReward requireReward(Long rewardId) {
        Long companyId = currentUserService.getCurrentCompanyId();
        return ambassadorRewardRepository.findByIdAndCompanyId(rewardId, companyId)
                .orElseThrow(() -> new NotFoundException("Reward not found"));
    }

    private AmbassadorRewardResponse toResponse(AmbassadorReward reward) {
        RevenueEvent revenueEvent = reward.getRevenueEvent();
        return new AmbassadorRewardResponse(
                reward.getId(),
                reward.getCampaign().getId(),
                reward.getCampaign().getName(),
                reward.getReferral().getId(),
                reward.getReferral().getReferralCode(),
                reward.getAmbassadorUser().getId(),
                ambassadorDisplayName(reward.getAmbassadorUser()),
                reward.getRewardType(),
                reward.getRewardValue(),
                reward.getCurrency(),
                reward.getStatus(),
                reward.getHoldReason(),
                reward.getRejectionReason(),
                revenueEvent.getId(),
                revenueEvent.getQualifyingStatus(),
                revenueEvent.getRevenueAmount(),
                revenueEvent.isCurrencyMismatch(),
                reward.getCreatedAt(),
                reward.getApprovedAt(),
                reward.getPaidAt(),
                reward.getRejectedAt(),
                reward.getReversedAt()
        );
    }

    private String ambassadorDisplayName(DashboardUser user) {
        String fullName = Stream.of(user.getFirstName(), user.getLastName())
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(value -> !value.isEmpty())
                .collect(Collectors.joining(" "));
        return fullName.isBlank() ? user.getUsername() : fullName;
    }
}
