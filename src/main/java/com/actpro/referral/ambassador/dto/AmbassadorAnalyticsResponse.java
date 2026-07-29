package com.actpro.referral.ambassador.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record AmbassadorAnalyticsResponse(
        LocalDate fromDate,
        LocalDate toDate,
        long totalClicks,
        long totalRegistrations,
        long totalBookingsStarted,
        long totalCompletedRentals,
        BigDecimal registrationConversionRate,
        BigDecimal rentalConversionRate,
        List<AmbassadorCampaignPerformanceResponse> campaigns,
        List<AmbassadorPerformanceTrendResponse> trends
) {
}
