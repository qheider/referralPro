package com.actpro.referral.ambassador.dto;

import com.actpro.referral.ambassador.AmbassadorStatus;

import java.time.LocalDateTime;

public record AmbassadorSummaryResponse(
        Long id,
        Long userId,
        String firstName,
        String lastName,
        String email,
        String displayName,
        AmbassadorStatus status,
        Long assignedCampaigns,
        Long totalRegistrations,
        Long successfulRentals,
        Double conversionRate,
        LocalDateTime createdAt
) {
}
