package com.actpro.referral.referral.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record GenerateReferralRequest(
        @NotNull(message = "Campaign ID is required")
        Long campaignId,

        @NotBlank(message = "External user ID is required")
        String externalUserId,

        @NotBlank(message = "Email is required")
        String email,

        String name
) {
}
