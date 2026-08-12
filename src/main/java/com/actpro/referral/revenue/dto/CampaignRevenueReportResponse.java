package com.actpro.referral.revenue.dto;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * Campaign-level revenue/reward rollup for {@code RevenueAdminController}. {@code revenueByCurrency}
 * only sums {@link com.actpro.referral.revenue.RevenueEvent} rows where {@code currencyMismatch}
 * is false and currency is present - the "explicit validation policy" for currency mismatch: never
 * silently blend a mismatched-currency amount into a total that claims to be in one currency.
 * {@code mismatchedCurrencyEventCount} surfaces how many rows were excluded so a mismatch is
 * visible rather than silently dropped.
 */
public record CampaignRevenueReportResponse(
        Long campaignId,
        String campaignName,
        long qualifyingEventCount,
        long reversedEventCount,
        long mismatchedCurrencyEventCount,
        Map<String, BigDecimal> revenueByCurrency,
        long rewardCount,
        BigDecimal totalPendingValue,
        BigDecimal totalEligibleValue,
        BigDecimal totalApprovedValue,
        BigDecimal totalPaidValue,
        BigDecimal totalRejectedValue,
        BigDecimal totalReversedValue,
        List<AmbassadorRevenueSummaryResponse> ambassadorLeaderboard
) {
}
