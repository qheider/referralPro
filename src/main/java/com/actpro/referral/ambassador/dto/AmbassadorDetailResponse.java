package com.actpro.referral.ambassador.dto;

import com.actpro.referral.ambassador.AmbassadorStatus;

import java.time.LocalDateTime;
import java.util.List;

public record AmbassadorDetailResponse(
        Long id,
        Long userId,
        String firstName,
        String lastName,
        String email,
        String displayName,
        String phone,
        String bio,
        String socialMediaPlatform,
        String socialMediaHandle,
        String profileImageUrl,
        String ambassadorCode,
        AmbassadorStatus status,
        Long assignedCampaigns,
        Long totalRegistrations,
        Long successfulRentals,
        Double conversionRate,
        LocalDateTime joinedAt,
        LocalDateTime createdAt,
        List<AmbassadorReferralLinkResponse> referralLinks
) {
}
