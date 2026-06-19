package com.actpro.referral.conversion.dto;

import jakarta.validation.constraints.NotBlank;

public record ConversionRequest(
        @NotBlank(message = "Referral code is required")
        String referralCode,

        @NotBlank(message = "External user ID is required")
        String externalUserId,

        @NotBlank(message = "Email is required")
        String email,

        String name,

        @NotBlank(message = "Event name is required")
        String eventName
) {
}
