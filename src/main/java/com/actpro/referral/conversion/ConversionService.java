package com.actpro.referral.conversion;

import com.actpro.referral.campaign.Campaign;
import com.actpro.referral.common.exception.BadRequestException;
import com.actpro.referral.common.exception.NotFoundException;
import com.actpro.referral.company.Company;
import com.actpro.referral.conversion.dto.ConversionRequest;
import com.actpro.referral.referral.Referral;
import com.actpro.referral.referral.ReferralCodeGenerator;
import com.actpro.referral.referral.ReferralLink;
import com.actpro.referral.referral.ReferralLinkRepository;
import com.actpro.referral.referral.ReferralLinkStatus;
import com.actpro.referral.referral.ReferralLinkUrlService;
import com.actpro.referral.referral.ReferralRepository;
import com.actpro.referral.referral.ReferralStatus;
import com.actpro.referral.revenue.RevenueEventService;
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
    private final ReferralLinkRepository referralLinkRepository;
    private final PlatformUserService platformUserService;
    private final RewardService rewardService;
    private final RevenueEventService revenueEventService;
    private final ReferralCodeGenerator referralCodeGenerator;
    private final ReferralLinkUrlService referralLinkUrlService;

    /**
     * {@code request.referralCode()} is resolved polymorphically: it's normally an existing
     * {@link Referral#getReferralCode()} (the legacy direct-API flow, and the default ambassador
     * flow where {@code ReferralLeadService} already created a {@link Referral} via ReferralPro's
     * own {@code /refer/{token}} lead capture). When it doesn't match any {@link Referral}, it's
     * treated as a {@link ReferralLink#getPublicToken()} instead - the
     * {@link Campaign#isDirectToLandingPageMode() direct-to-landing-page} case, where ReferralPro
     * never captured a click/lead itself, so no {@link Referral} exists yet; one is created here,
     * on the fly, from the {@link ReferralLink}.
     */
    @Transactional
    public ConversionWithRewards completeConversion(ConversionRequest request) {
        Company company = CompanyContext.getCurrentCompany();
        log.info("Processing conversion for company: {} (ID: {}), referralCode: {}",
                company.getName(), company.getId(), request.referralCode());

        Referral referral = referralRepository
                .findByReferralCodeAndCompanyId(request.referralCode(), company.getId())
                .orElse(null);

        // Resolve which campaign this is against and validate it before touching anything else
        // (PlatformUser creation included) - an unresolvable/inactive/wrong-event code should fail
        // fast with no side effects, same as before this method also handled the ReferralLink case.
        ReferralLink link = null;
        Campaign campaign;
        if (referral != null) {
            campaign = referral.getCampaign();
        } else {
            link = resolveActiveLinkByToken(request.referralCode(), company);
            campaign = link.getCampaign();
        }
        validateCampaignAndEvent(campaign, request.eventName());

        PlatformUser referee = platformUserService.findOrCreate(
                company, request.externalUserId(), request.email(), request.name());

        if (referral == null) {
            // Idempotency: a retried company report for the same registrant against the same link
            // must reuse the referral created by the first call, not create a second one (which
            // would otherwise slip past the duplicate-conversion check below, since that check is
            // keyed by referral id).
            ReferralLink resolvedLink = link;
            referral = referralRepository
                    .findByReferralLinkEntityIdAndCustomerUserId(resolvedLink.getId(), referee.getId())
                    .orElseGet(() -> createReferralFromLink(resolvedLink, referee));
        }

        // Self-referral only applies to the legacy PlatformUser-referrer model - an ambassador-driven
        // referral (referrerUser null, see Referral.referrerUser's Javadoc) has no comparable identity
        // to check against: a DashboardUser ambassador isn't linked to a PlatformUser account.
        if (referral.getReferrerUser() != null && referral.getReferrerUser().getId().equals(referee.getId())) {
            throw new BadRequestException("Self referral is not allowed");
        }

        boolean duplicate = conversionRepository.existsByReferralIdAndRefereeUserId(referral.getId(), referee.getId());
        if (duplicate) {
            throw new BadRequestException("Referral already converted for this user");
        }

        boolean ambassadorDriven = referral.getReferrerUser() == null;
        LocalDateTime now = LocalDateTime.now();
        if (ambassadorDriven) {
            referral.setStatus(ReferralStatus.CONVERTED);
            referral.setConvertedAt(now);
            referralRepository.save(referral);
        }

        Conversion conversion = new Conversion();
        conversion.setCompany(company);
        conversion.setCampaign(referral.getCampaign());
        conversion.setReferral(referral);
        conversion.setReferrerUser(referral.getReferrerUser());
        conversion.setRefereeUser(referee);
        conversion.setEventName(request.eventName());
        conversion.setStatus(ConversionStatus.COMPLETED);
        conversion.setCompletedAt(now);

        conversion = conversionRepository.save(conversion);
        log.info("Conversion created successfully with ID: {}", conversion.getId());

        // Issue rewards - legacy Reward/Conversion for a PlatformUser referrer, or (ambassador-
        // driven) an AmbassadorReward via RevenueEventService, the same pipeline the ambassador
        // portal's earnings screens already read from.
        RewardResult rewardResult = rewardService.issueRewards(conversion);
        if (ambassadorDriven) {
            rewardResult.setAmbassadorReward(revenueEventService.recordConversionQualifyingEvent(referral, now));
        }

        // Update conversion status to REWARDED
        conversion.setStatus(ConversionStatus.REWARDED);
        conversionRepository.save(conversion);

        return new ConversionWithRewards(conversion, rewardResult);
    }

    private ReferralLink resolveActiveLinkByToken(String publicToken, Company company) {
        ReferralLink link = referralLinkRepository.findDetailedByPublicToken(publicToken)
                .filter(l -> l.getCompany().getId().equals(company.getId()))
                .orElseThrow(() -> new NotFoundException("Referral not found"));

        // Same "not found" treatment as ReferralClickService/ReferralLeadService give a disabled or
        // expired link - don't leak link state to the caller beyond "this code doesn't resolve".
        if (link.getStatus() != ReferralLinkStatus.ACTIVE
                || (link.getExpiresAt() != null && link.getExpiresAt().isBefore(LocalDateTime.now()))) {
            throw new NotFoundException("Referral not found");
        }

        // Only a direct-to-landing-page campaign's links may be resolved this way - a link for a
        // campaign still using the default /r/{token} -> /refer/{token} lead-capture flow must NOT
        // be convertible by publicToken alone, or a company could fabricate a conversion (and an
        // AmbassadorReward) for a link that never actually went through that flow. Treated the same
        // as an unresolvable code, not a different error, so link existence isn't leaked either way.
        if (!link.getCampaign().isDirectToLandingPageMode()) {
            throw new NotFoundException("Referral not found");
        }
        return link;
    }

    private Referral createReferralFromLink(ReferralLink link, PlatformUser referee) {
        Referral referral = new Referral();
        referral.setCompany(link.getCompany());
        referral.setCampaign(link.getCampaign());
        referral.setAmbassadorUser(link.getAmbassadorUser());
        referral.setReferralLinkEntity(link);
        referral.setCustomerUser(referee);
        referral.setReferralCode(referralCodeGenerator.generateUniqueCode());
        // The actual URL the referred visitor followed - the direct landing-page URL in this mode,
        // not a /r/{token} link they never visited.
        referral.setReferralLink(referralLinkUrlService.resolveReferralUrl(link));
        referral.setStatus(ReferralStatus.REGISTERED);
        referral.setRegisteredAt(LocalDateTime.now());
        return referralRepository.save(referral);
    }

    private void validateCampaignAndEvent(Campaign campaign, String eventName) {
        if (!campaign.isActive()) {
            throw new BadRequestException("Campaign is not active");
        }
        if (!campaign.getConversionEventName().equals(eventName)) {
            throw new BadRequestException("Invalid conversion event. Expected: " + campaign.getConversionEventName());
        }
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
