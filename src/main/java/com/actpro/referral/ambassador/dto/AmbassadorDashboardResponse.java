package com.actpro.referral.ambassador.dto;

import java.math.BigDecimal;
import java.util.List;

public record AmbassadorDashboardResponse(
        Long ambassadorId,
        String displayName,
        long activeCampaigns,
        long totalClicks,
        long totalRegistrations,
        long totalBookingsStarted,
        long totalCompletedRentals,
        BigDecimal registrationConversionRate,
        BigDecimal rentalConversionRate,
        List<AmbassadorRecentReferralResponse> recentReferrals
) {
}
