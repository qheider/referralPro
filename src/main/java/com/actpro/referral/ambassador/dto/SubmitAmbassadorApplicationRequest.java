package com.actpro.referral.ambassador.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record SubmitAmbassadorApplicationRequest(
        @NotBlank(message = "First name is required")
        @Size(max = 100, message = "First name must be at most 100 characters")
        String firstName,

        @NotBlank(message = "Last name is required")
        @Size(max = 100, message = "Last name must be at most 100 characters")
        String lastName,

        @NotBlank(message = "Email is required")
        @Email(message = "Invalid email format")
        @Size(max = 100, message = "Email must be at most 100 characters")
        String email,

        @Size(max = 50, message = "Phone must be at most 50 characters")
        String phone,

        @Size(max = 255, message = "Display name must be at most 255 characters")
        String displayName,

        @Size(max = 2000, message = "Bio must be at most 2000 characters")
        String bio,

        @Size(max = 100, message = "Social media platform must be at most 100 characters")
        String socialMediaPlatform,

        @Size(max = 255, message = "Social media handle must be at most 255 characters")
        String socialMediaHandle
) {
}
