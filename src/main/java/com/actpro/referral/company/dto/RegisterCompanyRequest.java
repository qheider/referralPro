package com.actpro.referral.company.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record RegisterCompanyRequest(
        // Required Company Fields
        @NotBlank(message = "Company name is required")
        String companyName,

        @NotBlank(message = "Company email is required")
        @Email(message = "Invalid email format")
        String companyEmail,

        @NotBlank(message = "Company website is required")
        String companyWebsite,

        @NotBlank(message = "Industry is required")
        String industry,

        @NotBlank(message = "Country is required")
        String country,

        String taxId,

        // Required Admin Fields
        @NotBlank(message = "Admin full name is required")
        String adminFullName,

        @NotBlank(message = "Admin work email is required")
        @Email(message = "Invalid admin email format")
        String adminWorkEmail,

        @NotBlank(message = "Admin phone is required")
        String adminPhone,

        @NotBlank(message = "Password is required")
        @Size(min = 8, message = "Password must be at least 8 characters")
        String password,

        @NotBlank(message = "Admin role is required")
        String adminRole,

        // Required Business Fields
        @NotBlank(message = "Company size is required")
        String companySize,

        @NotBlank(message = "Preferred currency is required")
        String preferredCurrency,

        @NotNull(message = "Terms acceptance is required")
        Boolean acceptedTerms,

        // Optional Company Fields
        String companyLogoUrl,
        AddressDto address,
        String companyDescription,
        SocialLinksDto socialLinks,
        String registrationCertificateUrl,

        // Optional Contact Fields
        String secondaryContactName,
        String department,
        String referralProgramManager,

        // Optional Business Preferences
        Integer estimatedMonthlyReferralVolume,
        String rewardTypePreference,
        String referralSource,
        String signupReferralCode,
        String gstVatNumber,
        String industryLicenseNumber,
        Boolean dpaAccepted
) {
    public record AddressDto(
            String street,
            String city,
            String state,
            String zip
    ) {}

    public record SocialLinksDto(
            String linkedin,
            String twitter,
            String facebook
    ) {}
}
