package com.actpro.referral.ambassador.dto;

import java.math.BigDecimal;

public record AmbassadorCampaignPerformanceResponse(
        Long campaignId,
        String campaignName,
        long clicks,
        long registrations,
        long completedRentals,
        BigDecimal registrationConversionRate,
        BigDecimal rentalConversionRate
) {
}
