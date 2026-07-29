package com.actpro.referral.ambassador.dto;

import com.actpro.referral.ambassador.AmbassadorStatus;
import com.actpro.referral.auth.UserStatus;

import java.time.LocalDateTime;

public record AmbassadorProfileResponse(
        Long ambassadorId,
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
        AmbassadorStatus ambassadorStatus,
        UserStatus userStatus,
        LocalDateTime joinedAt
) {
}
