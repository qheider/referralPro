package com.actpro.referral.company;

import com.actpro.referral.auth.DashboardUser;
import com.actpro.referral.auth.DashboardUserRepository;
import com.actpro.referral.common.exception.BadRequestException;
import com.actpro.referral.company.dto.RegisterCompanyRequest;
import com.actpro.referral.company.dto.RegisterCompanyResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class CompanyService {

    private final CompanyRepository companyRepository;
    private final DashboardUserRepository dashboardUserRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public RegisterCompanyResponse registerCompany(RegisterCompanyRequest request) {
        log.info("Registering company with email: {}", request.companyEmail());

        // Validate terms acceptance
        if (request.acceptedTerms() == null || !request.acceptedTerms()) {
            throw new BadRequestException("You must accept the terms and conditions");
        }

        // Check if company email already exists
        if (companyRepository.existsByEmail(request.companyEmail())) {
            throw new BadRequestException("Company with this email already exists");
        }

        // Check if admin email already exists
        if (dashboardUserRepository.existsByUsername(request.adminWorkEmail())) {
            throw new BadRequestException("Admin email already exists");
        }

        // Create company
        Company company = new Company();
        
        // Required fields
        company.setName(request.companyName());
        company.setEmail(request.companyEmail());
        company.setWebsite(request.companyWebsite());
        company.setIndustry(request.industry());
        company.setCountry(request.country());
        company.setCompanySize(request.companySize());
        company.setPreferredCurrency(request.preferredCurrency());
        company.setStatus(CompanyStatus.ACTIVE);
        company.setApiKey(generateApiKey());
        
        // Optional company fields
        company.setTaxId(request.taxId());
        company.setCompanyLogoUrl(request.companyLogoUrl());
        company.setDescription(request.companyDescription());
        company.setRegistrationCertificateUrl(request.registrationCertificateUrl());
        company.setGstVatNumber(request.gstVatNumber());
        company.setIndustryLicenseNumber(request.industryLicenseNumber());
        company.setDpaAccepted(request.dpaAccepted());
        
        // Address
        if (request.address() != null) {
            company.setAddressStreet(request.address().street());
            company.setAddressCity(request.address().city());
            company.setAddressState(request.address().state());
            company.setAddressZip(request.address().zip());
        }
        
        // Social Links
        if (request.socialLinks() != null) {
            company.setLinkedinUrl(request.socialLinks().linkedin());
            company.setTwitterUrl(request.socialLinks().twitter());
            company.setFacebookUrl(request.socialLinks().facebook());
        }
        
        // Contact & Business fields
        company.setSecondaryContactName(request.secondaryContactName());
        company.setDepartment(request.department());
        company.setReferralProgramManager(request.referralProgramManager());
        company.setEstimatedMonthlyReferralVolume(request.estimatedMonthlyReferralVolume());
        company.setRewardTypePreference(request.rewardTypePreference());
        company.setReferralSource(request.referralSource());
        company.setSignupReferralCode(request.signupReferralCode());

        company = companyRepository.save(company);
        log.info("Company registered successfully with ID: {}", company.getId());

        // Create admin dashboard user
        DashboardUser adminUser = new DashboardUser();
        adminUser.setCompany(company);
        adminUser.setUsername(request.adminWorkEmail());
        adminUser.setPassword(passwordEncoder.encode(request.password()));
        adminUser.setRole(request.adminRole());
        
        dashboardUserRepository.save(adminUser);
        log.info("Admin user created for company: {}", company.getId());

        return new RegisterCompanyResponse(
                company.getId(),
                company.getName(),
                company.getApiKey(),
                request.adminWorkEmail()
        );
    }

    private String generateApiKey() {
        String uuid = UUID.randomUUID().toString().replace("-", "");
        return "cmp_live_" + uuid;
    }
}
