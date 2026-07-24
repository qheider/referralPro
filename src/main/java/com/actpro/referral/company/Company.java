package com.actpro.referral.company;

import com.actpro.referral.common.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "companies")
@Getter
@Setter
@NoArgsConstructor
public class Company extends BaseEntity {

    @Column(nullable = false)
    private String name;

    @Column(nullable = false, unique = true)
    private String email;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CompanyStatus status = CompanyStatus.ACTIVE;

    @Column(name = "api_key", nullable = false, unique = true)
    private String apiKey;

    // Company Details
    private String website;
    private String industry;
    private String country;
    
    @Column(name = "tax_id")
    private String taxId;
    
    @Column(name = "company_size")
    private String companySize;
    
    @Column(name = "preferred_currency")
    private String preferredCurrency;
    
    @Column(name = "company_logo_url", length = 500)
    private String companyLogoUrl;
    
    @Column(length = 1000)
    private String description;
    
    // Address fields
    @Column(name = "address_street")
    private String addressStreet;
    
    @Column(name = "address_city")
    private String addressCity;
    
    @Column(name = "address_state")
    private String addressState;
    
    @Column(name = "address_zip")
    private String addressZip;
    
    // Social Links
    @Column(name = "linkedin_url", length = 500)
    private String linkedinUrl;
    
    @Column(name = "twitter_url", length = 500)
    private String twitterUrl;
    
    @Column(name = "facebook_url", length = 500)
    private String facebookUrl;
    
    // Registration & Compliance
    @Column(name = "registration_certificate_url", length = 500)
    private String registrationCertificateUrl;
    
    @Column(name = "gst_vat_number")
    private String gstVatNumber;
    
    @Column(name = "industry_license_number")
    private String industryLicenseNumber;
    
    @Column(name = "dpa_accepted")
    private Boolean dpaAccepted;
    
    // Contact Details
    @Column(name = "secondary_contact_name")
    private String secondaryContactName;
    
    private String department;
    
    @Column(name = "referral_program_manager")
    private String referralProgramManager;
    
    // Business Preferences
    @Column(name = "estimated_monthly_volume")
    private Integer estimatedMonthlyReferralVolume;
    
    @Column(name = "reward_type_preference")
    private String rewardTypePreference;
    
    @Column(name = "referral_source")
    private String referralSource;
    
    @Column(name = "signup_referral_code")
    private String signupReferralCode;
}
