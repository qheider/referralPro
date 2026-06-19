package com.actpro.referral.conversion;

import com.actpro.referral.campaign.Campaign;
import com.actpro.referral.common.exception.BadRequestException;
import com.actpro.referral.common.exception.NotFoundException;
import com.actpro.referral.company.Company;
import com.actpro.referral.conversion.dto.ConversionRequest;
import com.actpro.referral.referral.Referral;
import com.actpro.referral.referral.ReferralRepository;
import com.actpro.referral.reward.RewardService;
import com.actpro.referral.reward.dto.RewardResult;
import com.actpro.referral.security.CompanyContext;
import com.actpro.referral.user.PlatformUser;
import com.actpro.referral.user.PlatformUserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class ConversionService {

    private final ConversionRepository conversionRepository;
    private final ReferralRepository referralRepository;
    private final PlatformUserService platformUserService;
    private final RewardService rewardService;
    private final CompanyContext companyContext;

    @Transactional
    public ConversionWithRewards completeConversion(ConversionRequest request) {
        Company company = companyContext.getCurrentCompany();
        log.info("Processing conversion for company: {} (ID: {}), referralCode: {}",
                company.getName(), company.getId(), request.referralCode());

        // Find referral
        Referral referral = referralRepository
                .findByReferralCodeAndCompanyId(request.referralCode(), company.getId())
                .orElseThrow(() -> new NotFoundException("Referral not found"));

        Campaign campaign = referral.getCampaign();

        // Validate campaign is active
        if (!campaign.isActive()) {
            throw new BadRequestException("Campaign is not active");
        }

        // Validate event name matches
        if (!campaign.getConversionEventName().equals(request.eventName())) {
            throw new BadRequestException("Invalid conversion event. Expected: " + campaign.getConversionEventName());
        }

        // Find or create referee user
        PlatformUser referee = platformUserService.findOrCreate(
                company,
                request.externalUserId(),
                request.email(),
                request.name()
        );

        // Validate not self-referral
        if (referral.getReferrerUser().getId().equals(referee.getId())) {
            throw new BadRequestException("Self referral is not allowed");
        }

        // Check for duplicate conversion
        boolean duplicate = conversionRepository.existsByReferralIdAndRefereeUserId(
                referral.getId(),
                referee.getId()
        );

        if (duplicate) {
            throw new BadRequestException("Referral already converted for this user");
        }

        // Create conversion
        Conversion conversion = new Conversion();
        conversion.setCompany(company);
        conversion.setCampaign(campaign);
        conversion.setReferral(referral);
        conversion.setReferrerUser(referral.getReferrerUser());
        conversion.setRefereeUser(referee);
        conversion.setEventName(request.eventName());
        conversion.setStatus(ConversionStatus.COMPLETED);
        conversion.setCompletedAt(LocalDateTime.now());

        conversion = conversionRepository.save(conversion);
        log.info("Conversion created successfully with ID: {}", conversion.getId());

        // Issue rewards
        RewardResult rewardResult = rewardService.issueRewards(conversion);

        // Update conversion status to REWARDED
        conversion.setStatus(ConversionStatus.REWARDED);
        conversionRepository.save(conversion);

        return new ConversionWithRewards(conversion, rewardResult);
    }

    // Wrapper class to return both conversion and rewards
    public static class ConversionWithRewards {
        private final Conversion conversion;
        private final RewardResult rewardResult;

        public ConversionWithRewards(Conversion conversion, RewardResult rewardResult) {
            this.conversion = conversion;
            this.rewardResult = rewardResult;
        }

        public Conversion getConversion() {
            return conversion;
        }

        public RewardResult getRewardResult() {
            return rewardResult;
        }
    }
}
