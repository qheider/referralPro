package com.actpro.referral.revenue.dto;

import java.math.BigDecimal;

/** One leaderboard row within a {@link CampaignRevenueReportResponse}. */
public record AmbassadorRevenueSummaryResponse(
        Long ambassadorUserId,
        String ambassadorName,
        long qualifyingEventCount,
        long reversedEventCount,
        BigDecimal totalPendingOrEligibleValue,
        BigDecimal totalApprovedValue,
        BigDecimal totalPaidValue
) {
}
