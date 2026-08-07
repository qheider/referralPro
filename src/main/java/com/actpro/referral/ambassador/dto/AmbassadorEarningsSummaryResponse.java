package com.actpro.referral.ambassador.dto;

import java.math.BigDecimal;

public record AmbassadorEarningsSummaryResponse(
        BigDecimal totalPaid,
        BigDecimal totalApproved,
        BigDecimal totalPendingOrEligible,
        BigDecimal totalRejectedOrReversed,
        long rewardCount,
        String currency
) {
}
