package com.actpro.referral.ambassador.dto;

import jakarta.validation.constraints.Size;

public record UpdateAmbassadorProfileRequest(
        @Size(max = 100) String firstName,
        @Size(max = 100) String lastName,
        @Size(max = 255) String displayName,
        @Size(max = 50) String phone,
        @Size(max = 2000) String bio,
        @Size(max = 100) String socialMediaPlatform,
        @Size(max = 255) String socialMediaHandle,
        @Size(max = 500) String profileImageUrl
) {
}
