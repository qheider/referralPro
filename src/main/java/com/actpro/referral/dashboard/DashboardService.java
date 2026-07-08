package com.actpro.referral.dashboard;

import com.actpro.referral.campaign.Campaign;
import com.actpro.referral.campaign.CampaignRepository;
import com.actpro.referral.common.exception.NotFoundException;
import com.actpro.referral.company.Company;
import com.actpro.referral.dashboard.dto.*;
import com.actpro.referral.security.CompanyContext;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class DashboardService {

    private final CampaignRepository campaignRepository;
    private final EntityManager entityManager;

    @Transactional(readOnly = true)
    public CampaignsOverviewResponse getCampaignsOverview() {
        Company company = CompanyContext.getCurrentCompany();
        
        if (company == null) {
            throw new IllegalStateException("Company context not set - authentication may have failed");
        }
        
        log.debug("Loading campaigns overview for company: {} (ID: {})", company.getName(), company.getId());

        // Get all campaigns for the company
        List<Campaign> campaigns = campaignRepository.findByCompanyId(company.getId());
        log.debug("Found {} campaigns for company", campaigns.size());

        List<CampaignOverviewItem> campaignItems = campaigns.stream()
                .map(campaign -> {
                    // Count referrals and clicks for this campaign
                    String referralSql = """
                        SELECT 
                            COUNT(DISTINCT r.id) as referralCount,
                            COUNT(DISTINCT rc.id) as clickCount
                        FROM referrals r
                        LEFT JOIN referral_clicks rc ON rc.referral_id = r.id
                        WHERE r.campaign_id = :campaignId
                        """;
                    Query referralQuery = entityManager.createNativeQuery(referralSql);
                    referralQuery.setParameter("campaignId", campaign.getId());
                    Object[] referralResult = (Object[]) referralQuery.getSingleResult();
                    
                    Long referralCount = referralResult[0] != null ? ((Number) referralResult[0]).longValue() : 0L;
                    Long clickCount = referralResult[1] != null ? ((Number) referralResult[1]).longValue() : 0L;

                    // Count conversions for this campaign
                    String conversionSql = """
                        SELECT COUNT(DISTINCT id) FROM conversions WHERE campaign_id = :campaignId
                        """;
                    Query conversionQuery = entityManager.createNativeQuery(conversionSql);
                    conversionQuery.setParameter("campaignId", campaign.getId());
                    Long conversionCount = ((Number) conversionQuery.getSingleResult()).longValue();

                    Double conversionRate = referralCount > 0
                            ? (conversionCount * 100.0 / referralCount)
                            : 0.0;

                    return new CampaignOverviewItem(
                            campaign.getId(),
                            campaign.getName(),
                            campaign.getStatus().name(),
                            referralCount,
                            clickCount,
                            conversionCount,
                            Math.round(conversionRate * 100.0) / 100.0
                    );
                })
                .collect(Collectors.toList());

        CampaignsOverviewResponse response = new CampaignsOverviewResponse(
                company.getId(),
                company.getName(),
                campaignItems
        );
        
        log.debug("Returning campaigns overview with {} campaigns", campaignItems.size());
        return response;
    }

    @Transactional(readOnly = true)
    public CampaignStatsResponse getCampaignStats(Long campaignId) {
        Company company = CompanyContext.getCurrentCompany();
        Campaign campaign = campaignRepository.findByIdAndCompanyId(campaignId, company.getId())
                .orElseThrow(() -> new NotFoundException("Campaign not found"));

        String sql = """
            SELECT 
                COUNT(DISTINCT r.id) as totalReferrals,
                COUNT(DISTINCT rc.id) as totalClicks,
                COUNT(DISTINCT conv.id) as totalConversions,
                COUNT(DISTINCT rw.id) as totalRewards,
                COALESCE(SUM(CASE WHEN rw.id IS NOT NULL THEN rw.reward_value ELSE 0 END), 0) as totalRewardValue,
                COUNT(DISTINCT CASE WHEN rw.status = 'REDEEMED' THEN rw.id END) as rewardsRedeemed
            FROM campaigns c
            LEFT JOIN referrals r ON r.campaign_id = c.id
            LEFT JOIN referral_clicks rc ON rc.referral_id = r.id
            LEFT JOIN conversions conv ON conv.campaign_id = c.id
            LEFT JOIN rewards rw ON rw.campaign_id = c.id
            WHERE c.id = :campaignId AND c.company_id = :companyId
            """;

        Query query = entityManager.createNativeQuery(sql);
        query.setParameter("campaignId", campaignId);
        query.setParameter("companyId", company.getId());

        Object[] result = (Object[]) query.getSingleResult();

        Long totalReferrals = ((Number) result[0]).longValue();
        Long totalClicks = ((Number) result[1]).longValue();
        Long totalConversions = ((Number) result[2]).longValue();
        Long totalRewards = ((Number) result[3]).longValue();
        BigDecimal totalRewardValue = new BigDecimal(result[4].toString());
        Long rewardsRedeemed = ((Number) result[5]).longValue();

        Double clickThroughRate = totalReferrals > 0
                ? (totalClicks * 100.0 / totalReferrals)
                : 0.0;

        Double conversionRate = totalClicks > 0
                ? (totalConversions * 100.0 / totalClicks)
                : 0.0;

        BigDecimal averageRewardValue = totalRewards > 0
                ? totalRewardValue.divide(BigDecimal.valueOf(totalRewards), 2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        return new CampaignStatsResponse(
                campaign.getId(),
                campaign.getName(),
                campaign.getStatus().name(),
                totalReferrals,
                totalClicks,
                totalConversions,
                Math.round(clickThroughRate * 100.0) / 100.0,
                Math.round(conversionRate * 100.0) / 100.0,
                totalRewards,
                totalRewardValue,
                rewardsRedeemed,
                averageRewardValue
        );
    }

    @Transactional(readOnly = true)
    public ConversionFunnelResponse getConversionFunnel(Long campaignId) {
        Company company = CompanyContext.getCurrentCompany();
        Campaign campaign = campaignRepository.findByIdAndCompanyId(campaignId, company.getId())
                .orElseThrow(() -> new NotFoundException("Campaign not found"));

        String sql = """
            SELECT 
                COUNT(DISTINCT r.id) as referralsCount,
                COUNT(DISTINCT rc.id) as clicksCount,
                COUNT(DISTINCT conv.id) as conversionsCount
            FROM campaigns c
            LEFT JOIN referrals r ON r.campaign_id = c.id
            LEFT JOIN referral_clicks rc ON rc.referral_id = r.id
            LEFT JOIN conversions conv ON conv.campaign_id = c.id
            WHERE c.id = :campaignId AND c.company_id = :companyId
            """;

        Query query = entityManager.createNativeQuery(sql);
        query.setParameter("campaignId", campaignId);
        query.setParameter("companyId", company.getId());

        Object[] result = (Object[]) query.getSingleResult();

        Long referralsCount = ((Number) result[0]).longValue();
        Long clicksCount = ((Number) result[1]).longValue();
        Long conversionsCount = ((Number) result[2]).longValue();

        Double clickThroughRate = referralsCount > 0
                ? (clicksCount * 100.0 / referralsCount)
                : 0.0;

        Double conversionRate = clicksCount > 0
                ? (conversionsCount * 100.0 / clicksCount)
                : 0.0;

        Double overallConversionRate = referralsCount > 0
                ? (conversionsCount * 100.0 / referralsCount)
                : 0.0;

        return new ConversionFunnelResponse(
                campaign.getId(),
                campaign.getName(),
                referralsCount,
                clicksCount,
                conversionsCount,
                Math.round(clickThroughRate * 100.0) / 100.0,
                Math.round(conversionRate * 100.0) / 100.0,
                Math.round(overallConversionRate * 100.0) / 100.0
        );
    }

    @Transactional(readOnly = true)
    public TopReferrersResponse getTopReferrers(Long campaignId, Integer limit) {
        Company company = CompanyContext.getCurrentCompany();
        Campaign campaign = campaignRepository.findByIdAndCompanyId(campaignId, company.getId())
                .orElseThrow(() -> new NotFoundException("Campaign not found"));

        if (limit == null || limit <= 0) {
            limit = 10;
        }

        String sql = """
            SELECT 
                pu.external_user_id,
                pu.name,
                pu.email,
                COALESCE(r_stats.referralCount, 0) as referralCount,
                COALESCE(r_stats.clickCount, 0) as clickCount,
                COALESCE(conv_stats.conversionCount, 0) as conversionCount,
                COALESCE(rw_stats.totalRewards, 0) as totalRewards
            FROM platform_users pu
            LEFT JOIN (
                SELECT 
                    r.referrer_user_id,
                    COUNT(DISTINCT r.id) as referralCount,
                    COUNT(DISTINCT rc.id) as clickCount
                FROM referrals r
                LEFT JOIN referral_clicks rc ON rc.referral_id = r.id
                WHERE r.campaign_id = :campaignId
                GROUP BY r.referrer_user_id
            ) r_stats ON r_stats.referrer_user_id = pu.id
            LEFT JOIN (
                SELECT 
                    referrer_user_id,
                    COUNT(DISTINCT id) as conversionCount
                FROM conversions
                WHERE campaign_id = :campaignId
                GROUP BY referrer_user_id
            ) conv_stats ON conv_stats.referrer_user_id = pu.id
            LEFT JOIN (
                SELECT 
                    user_id,
                    SUM(reward_value) as totalRewards
                FROM rewards
                WHERE campaign_id = :campaignId
                GROUP BY user_id
            ) rw_stats ON rw_stats.user_id = pu.id
            WHERE pu.company_id = :companyId
              AND COALESCE(r_stats.referralCount, 0) > 0
            ORDER BY COALESCE(conv_stats.conversionCount, 0) DESC, 
                     COALESCE(r_stats.referralCount, 0) DESC
            LIMIT :limit
            """;

        Query query = entityManager.createNativeQuery(sql);
        query.setParameter("campaignId", campaignId);
        query.setParameter("companyId", company.getId());
        query.setParameter("limit", limit);

        @SuppressWarnings("unchecked")
        List<Object[]> results = query.getResultList();

        List<TopReferrerItem> topReferrers = results.stream()
                .map(row -> new TopReferrerItem(
                        (String) row[0], // externalUserId
                        (String) row[1], // userName
                        (String) row[2], // userEmail
                        ((Number) row[3]).longValue(), // referralCount
                        ((Number) row[4]).longValue(), // clickCount
                        ((Number) row[5]).longValue(), // conversionCount
                        new BigDecimal(row[6].toString()) // totalRewards
                ))
                .collect(Collectors.toList());

        return new TopReferrersResponse(
                campaign.getId(),
                campaign.getName(),
                topReferrers
        );
    }

    @Transactional(readOnly = true)
    public TimeSeriesResponse getTimeSeries(Long campaignId, LocalDate startDate, LocalDate endDate, String granularity) {
        Company company = CompanyContext.getCurrentCompany();
        Campaign campaign = campaignRepository.findByIdAndCompanyId(campaignId, company.getId())
                .orElseThrow(() -> new NotFoundException("Campaign not found"));

        // Default to last 30 days if not specified
        if (endDate == null) {
            endDate = LocalDate.now();
        }
        if (startDate == null) {
            startDate = endDate.minusDays(30);
        }
        if (granularity == null) {
            granularity = "daily";
        }

        String sql = """
            SELECT 
                DATE(r.created_at) as date,
                COUNT(DISTINCT r.id) as referrals,
                0 as clicks,
                0 as conversions,
                0 as rewards
            FROM referrals r
            WHERE r.campaign_id = :campaignId
                AND DATE(r.created_at) BETWEEN :startDate AND :endDate
            GROUP BY DATE(r.created_at)
            
            UNION ALL
            
            SELECT 
                DATE(rc.clicked_at) as date,
                0 as referrals,
                COUNT(DISTINCT rc.id) as clicks,
                0 as conversions,
                0 as rewards
            FROM referral_clicks rc
            JOIN referrals r ON r.id = rc.referral_id
            WHERE r.campaign_id = :campaignId
                AND DATE(rc.clicked_at) BETWEEN :startDate AND :endDate
            GROUP BY DATE(rc.clicked_at)
            
            UNION ALL
            
            SELECT 
                DATE(conv.created_at) as date,
                0 as referrals,
                0 as clicks,
                COUNT(DISTINCT conv.id) as conversions,
                0 as rewards
            FROM conversions conv
            WHERE conv.campaign_id = :campaignId
                AND DATE(conv.created_at) BETWEEN :startDate AND :endDate
            GROUP BY DATE(conv.created_at)
            
            UNION ALL
            
            SELECT 
                DATE(rw.created_at) as date,
                0 as referrals,
                0 as clicks,
                0 as conversions,
                COUNT(DISTINCT rw.id) as rewards
            FROM rewards rw
            WHERE rw.campaign_id = :campaignId
                AND DATE(rw.created_at) BETWEEN :startDate AND :endDate
            GROUP BY DATE(rw.created_at)
            
            ORDER BY date
            """;

        Query query = entityManager.createNativeQuery(sql);
        query.setParameter("campaignId", campaignId);
        query.setParameter("startDate", startDate);
        query.setParameter("endDate", endDate);

        @SuppressWarnings("unchecked")
        List<Object[]> results = query.getResultList();

        // Aggregate by date
        List<TimeSeriesDataPoint> dataPoints = new ArrayList<>();
        LocalDate currentDate = null;
        Long referralsSum = 0L;
        Long clicksSum = 0L;
        Long conversionsSum = 0L;
        Long rewardsSum = 0L;

        for (Object[] row : results) {
            LocalDate date = ((java.sql.Date) row[0]).toLocalDate();
            Long referrals = ((Number) row[1]).longValue();
            Long clicks = ((Number) row[2]).longValue();
            Long conversions = ((Number) row[3]).longValue();
            Long rewards = ((Number) row[4]).longValue();

            if (currentDate == null) {
                currentDate = date;
            }

            if (!currentDate.equals(date)) {
                dataPoints.add(new TimeSeriesDataPoint(currentDate, referralsSum, clicksSum, conversionsSum, rewardsSum));
                currentDate = date;
                referralsSum = 0L;
                clicksSum = 0L;
                conversionsSum = 0L;
                rewardsSum = 0L;
            }

            referralsSum += referrals;
            clicksSum += clicks;
            conversionsSum += conversions;
            rewardsSum += rewards;
        }

        // Add last date
        if (currentDate != null) {
            dataPoints.add(new TimeSeriesDataPoint(currentDate, referralsSum, clicksSum, conversionsSum, rewardsSum));
        }

        return new TimeSeriesResponse(
                campaign.getId(),
                campaign.getName(),
                startDate,
                endDate,
                granularity,
                dataPoints
        );
    }

    @Transactional(readOnly = true)
    public RewardSummaryResponse getRewardSummary(Long campaignId) {
        Company company = CompanyContext.getCurrentCompany();
        Campaign campaign = campaignRepository.findByIdAndCompanyId(campaignId, company.getId())
                .orElseThrow(() -> new NotFoundException("Campaign not found"));

        // Get overall stats
        String statsSql = """
            SELECT 
                COUNT(DISTINCT rw.id) as totalIssued,
                COUNT(DISTINCT CASE WHEN rw.status = 'REDEEMED' THEN rw.id END) as totalRedeemed,
                COALESCE(SUM(rw.reward_value), 0) as totalValueIssued,
                COALESCE(SUM(CASE WHEN rw.status = 'REDEEMED' THEN rw.reward_value ELSE 0 END), 0) as totalValueRedeemed
            FROM rewards rw
            WHERE rw.campaign_id = :campaignId AND rw.company_id = :companyId
            """;

        Query statsQuery = entityManager.createNativeQuery(statsSql);
        statsQuery.setParameter("campaignId", campaignId);
        statsQuery.setParameter("companyId", company.getId());

        Object[] statsResult = (Object[]) statsQuery.getSingleResult();

        Long totalIssued = ((Number) statsResult[0]).longValue();
        Long totalRedeemed = ((Number) statsResult[1]).longValue();
        BigDecimal totalValueIssued = new BigDecimal(statsResult[2].toString());
        BigDecimal totalValueRedeemed = new BigDecimal(statsResult[3].toString());

        Double redemptionRate = totalIssued > 0
                ? (totalRedeemed * 100.0 / totalIssued)
                : 0.0;

        // Get breakdown by type
        String breakdownSql = """
            SELECT 
                rw.reward_type,
                COUNT(DISTINCT rw.id) as count,
                COALESCE(SUM(rw.reward_value), 0) as totalValue
            FROM rewards rw
            WHERE rw.campaign_id = :campaignId AND rw.company_id = :companyId
            GROUP BY rw.reward_type
            """;

        Query breakdownQuery = entityManager.createNativeQuery(breakdownSql);
        breakdownQuery.setParameter("campaignId", campaignId);
        breakdownQuery.setParameter("companyId", company.getId());

        @SuppressWarnings("unchecked")
        List<Object[]> breakdownResults = breakdownQuery.getResultList();

        List<RewardTypeBreakdown> breakdown = breakdownResults.stream()
                .map(row -> new RewardTypeBreakdown(
                        (String) row[0],
                        ((Number) row[1]).longValue(),
                        new BigDecimal(row[2].toString())
                ))
                .collect(Collectors.toList());

        return new RewardSummaryResponse(
                campaign.getId(),
                campaign.getName(),
                totalIssued,
                totalRedeemed,
                Math.round(redemptionRate * 100.0) / 100.0,
                totalValueIssued,
                totalValueRedeemed,
                breakdown
        );
    }
}
