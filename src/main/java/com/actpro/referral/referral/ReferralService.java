package com.actpro.referral.referral;

import com.actpro.referral.campaign.Campaign;
import com.actpro.referral.campaign.CampaignRepository;
import com.actpro.referral.common.exception.BadRequestException;
import com.actpro.referral.common.exception.NotFoundException;
import com.actpro.referral.company.Company;
import com.actpro.referral.referral.dto.GenerateReferralRequest;
import com.actpro.referral.referral.dto.GenerateReferralResponse;
import com.actpro.referral.security.CompanyContext;
import com.actpro.referral.user.PlatformUser;
import com.actpro.referral.user.PlatformUserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReferralService {

    private final ReferralRepository referralRepository;
    private final CampaignRepository campaignRepository;
    private final PlatformUserService platformUserService;
    private final ReferralCodeGenerator referralCodeGenerator;

    @Value("${app.base-url}")
    private String baseUrl;

    @Transactional
    public GenerateReferralResponse generateReferral(GenerateReferralRequest request) {
        Company company = CompanyContext.getCurrentCompany();
        log.info("Generating referral for company: {} (ID: {}), campaign: {}",
                company.getName(), company.getId(), request.campaignId());

        // Find campaign
        Campaign campaign = campaignRepository
                .findByIdAndCompanyId(request.campaignId(), company.getId())
                .orElseThrow(() -> new NotFoundException("Campaign not found"));

        // Validate campaign is active
        if (!campaign.isActive()) {
            throw new BadRequestException("Campaign is not active");
        }

        // Find or create referrer user
        PlatformUser referrer = platformUserService.findOrCreate(
                company,
                request.externalUserId(),
                request.email(),
                request.name()
        );

        // Generate unique referral code
        String code = referralCodeGenerator.generateUniqueCode();
        String link = baseUrl + "/r/" + code;

        // Create referral
        Referral referral = new Referral();
        referral.setCompany(company);
        referral.setCampaign(campaign);
        referral.setReferrerUser(referrer);
        referral.setReferralCode(code);
        referral.setReferralLink(link);
        referral.setStatus(ReferralStatus.ACTIVE);

        referralRepository.save(referral);
        log.info("Referral generated successfully: code={}, link={}", code, link);

        return new GenerateReferralResponse(code, link);
    }
}
