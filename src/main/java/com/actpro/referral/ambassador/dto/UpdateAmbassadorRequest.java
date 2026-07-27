package com.actpro.referral.ambassador.dto;

import com.actpro.referral.ambassador.AmbassadorStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record UpdateAmbassadorRequest(
        @NotBlank(message = "First name is required")
        @Size(max = 100, message = "First name must be at most 100 characters")
        String firstName,

        @NotBlank(message = "Last name is required")
        @Size(max = 100, message = "Last name must be at most 100 characters")
        String lastName,

        @Size(max = 255, message = "Display name must be at most 255 characters")
        String displayName,

        @Size(max = 50, message = "Phone must be at most 50 characters")
        String phone,

        @NotNull(message = "Status is required")
        AmbassadorStatus status,

        @Size(max = 100, message = "Social media platform must be at most 100 characters")
        String socialMediaPlatform,

        @Size(max = 255, message = "Social media handle must be at most 255 characters")
        String socialMediaHandle,

        String bio,

        @Size(max = 500, message = "Profile image URL must be at most 500 characters")
        String profileImageUrl
) {
}
