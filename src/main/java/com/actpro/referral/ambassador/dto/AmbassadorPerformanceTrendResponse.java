package com.actpro.referral.ambassador.dto;

import java.time.LocalDate;

public record AmbassadorPerformanceTrendResponse(
        LocalDate date,
        long clicks,
        long registrations,
        long completedRentals
) {
}
