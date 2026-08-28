package com.actpro.referral.conversion;

import com.actpro.referral.auth.DashboardUser;
import com.actpro.referral.campaign.Campaign;
import com.actpro.referral.campaign.CampaignStatus;
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
import com.actpro.referral.revenue.AmbassadorReward;
import com.actpro.referral.revenue.RevenueEventService;
import com.actpro.referral.reward.Reward;
import com.actpro.referral.reward.RewardService;
import com.actpro.referral.reward.dto.RewardResult;
import com.actpro.referral.security.CompanyContext;
import com.actpro.referral.user.PlatformUser;
import com.actpro.referral.user.PlatformUserService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ConversionServiceTest {

    @Mock
    private ConversionRepository conversionRepository;

    @Mock
    private ReferralRepository referralRepository;

    @Mock
    private ReferralLinkRepository referralLinkRepository;

    @Mock
    private PlatformUserService platformUserService;

    @Mock
    private RewardService rewardService;

    @Mock
    private RevenueEventService revenueEventService;

    @Mock
    private ReferralCodeGenerator referralCodeGenerator;

    @Mock
    private ReferralLinkUrlService referralLinkUrlService;

    @InjectMocks
    private ConversionService conversionService;

    private Company company;
    private Campaign campaign;
    private PlatformUser referee;

    @BeforeEach
    void setUp() {
        company = new Company();
        company.setId(5L);
        company.setName("Acme");
        CompanyContext.setCurrentCompany(company);

        campaign = new Campaign();
        campaign.setId(3L);
        campaign.setCompany(company);
        campaign.setStatus(CampaignStatus.ACTIVE);
        campaign.setStartDate(LocalDateTime.now().minusDays(1));
        campaign.setEndDate(LocalDateTime.now().plusDays(30));
        campaign.setConversionEventName("booking_completed");
        // Only relevant to the ReferralLink-publicToken resolution path (resolveActiveLinkByToken),
        // which gates on this - irrelevant to (and harmless for) the referralCode-lookup tests below.
        campaign.setDirectToLandingPageEnabled(true);
        campaign.setLandingPageUrl("https://company.example.com/signup");

        referee = new PlatformUser();
        referee.setId(40L);
    }

    @AfterEach
    void tearDown() {
        CompanyContext.clear();
    }

    private ConversionRequest request(String code) {
        return new ConversionRequest(code, "ext-1", "referee@example.com", "Referee", "booking_completed");
    }

    @Test
    void shouldCompleteLegacyDirectApiConversionUnchanged() {
        PlatformUser referrer = new PlatformUser();
        referrer.setId(10L);

        Referral referral = new Referral();
        referral.setId(9L);
        referral.setCompany(company);
        referral.setCampaign(campaign);
        referral.setReferralCode("LEGACY01");
        referral.setReferrerUser(referrer);
        referral.setStatus(ReferralStatus.ACTIVE);

        when(referralRepository.findByReferralCodeAndCompanyId("LEGACY01", 5L)).thenReturn(Optional.of(referral));
        when(platformUserService.findOrCreate(company, "ext-1", "referee@example.com", "Referee")).thenReturn(referee);
        when(conversionRepository.existsByReferralIdAndRefereeUserId(9L, 40L)).thenReturn(false);
        when(conversionRepository.save(any(Conversion.class))).thenAnswer(inv -> {
            Conversion c = inv.getArgument(0);
            if (c.getId() == null) {
                c.setId(100L);
            }
            return c;
        });
        when(rewardService.issueRewards(any(Conversion.class))).thenReturn(new RewardResult(null, null, null));

        ConversionService.ConversionWithRewards result = conversionService.completeConversion(request("LEGACY01"));

        assertEquals(referrer, result.getConversion().getReferrerUser());
        assertEquals(ConversionStatus.REWARDED, result.getConversion().getStatus());
        // Legacy flow never touches referral.status or the AmbassadorReward pipeline.
        assertEquals(ReferralStatus.ACTIVE, referral.getStatus());
        verify(revenueEventService, never()).recordConversionQualifyingEvent(any(), any());
        verify(referralLinkRepository, never()).findDetailedByPublicToken(any());
    }

    @Test
    void shouldRejectSelfReferralForLegacyFlow() {
        PlatformUser referrer = new PlatformUser();
        referrer.setId(40L); // same id as referee

        Referral referral = new Referral();
        referral.setId(9L);
        referral.setCampaign(campaign);
        referral.setReferrerUser(referrer);
        referral.setReferralCode("LEGACY01");

        when(referralRepository.findByReferralCodeAndCompanyId("LEGACY01", 5L)).thenReturn(Optional.of(referral));
        when(platformUserService.findOrCreate(company, "ext-1", "referee@example.com", "Referee")).thenReturn(referee);

        assertThrows(BadRequestException.class, () -> conversionService.completeConversion(request("LEGACY01")));

        verify(conversionRepository, never()).save(any());
    }

    @Test
    void shouldRejectDuplicateConversion() {
        PlatformUser referrer = new PlatformUser();
        referrer.setId(11L);

        Referral referral = new Referral();
        referral.setId(9L);
        referral.setCampaign(campaign);
        referral.setReferrerUser(referrer);
        referral.setReferralCode("LEGACY01");

        when(referralRepository.findByReferralCodeAndCompanyId("LEGACY01", 5L)).thenReturn(Optional.of(referral));
        when(platformUserService.findOrCreate(company, "ext-1", "referee@example.com", "Referee")).thenReturn(referee);
        when(conversionRepository.existsByReferralIdAndRefereeUserId(9L, 40L)).thenReturn(true);

        assertThrows(BadRequestException.class, () -> conversionService.completeConversion(request("LEGACY01")));

        verify(conversionRepository, never()).save(any());
    }

    @Test
    void shouldCompleteAmbassadorDrivenConversionForReferralFoundByReferralCode() {
        DashboardUser ambassador = new DashboardUser();
        ambassador.setId(77L);

        Referral referral = new Referral();
        referral.setId(9L);
        referral.setCompany(company);
        referral.setCampaign(campaign);
        referral.setReferralCode("LEAD1234");
        referral.setReferrerUser(null);
        referral.setAmbassadorUser(ambassador);
        referral.setStatus(ReferralStatus.REGISTERED);

        AmbassadorReward ambassadorReward = new AmbassadorReward();
        ambassadorReward.setId(500L);

        when(referralRepository.findByReferralCodeAndCompanyId("LEAD1234", 5L)).thenReturn(Optional.of(referral));
        when(platformUserService.findOrCreate(company, "ext-1", "referee@example.com", "Referee")).thenReturn(referee);
        when(conversionRepository.existsByReferralIdAndRefereeUserId(9L, 40L)).thenReturn(false);
        when(conversionRepository.save(any(Conversion.class))).thenAnswer(inv -> {
            Conversion c = inv.getArgument(0);
            if (c.getId() == null) {
                c.setId(101L);
            }
            return c;
        });
        when(rewardService.issueRewards(any(Conversion.class))).thenReturn(new RewardResult(null, new Reward(), null));
        when(revenueEventService.recordConversionQualifyingEvent(eq(referral), any())).thenReturn(ambassadorReward);

        ConversionService.ConversionWithRewards result = conversionService.completeConversion(request("LEAD1234"));

        assertNull(result.getConversion().getReferrerUser());
        assertEquals(referee, result.getConversion().getRefereeUser());
        assertEquals(ReferralStatus.CONVERTED, referral.getStatus());
        assertNotNull(referral.getConvertedAt());
        assertEquals(ambassadorReward, result.getRewardResult().getAmbassadorReward());
        verify(referralLinkRepository, never()).findDetailedByPublicToken(any());
    }

    @Test
    void shouldCreateReferralOnTheFlyWhenCodeResolvesToReferralLinkPublicToken() {
        DashboardUser ambassador = new DashboardUser();
        ambassador.setId(77L);

        ReferralLink link = new ReferralLink();
        link.setId(200L);
        link.setCompany(company);
        link.setCampaign(campaign);
        link.setAmbassadorUser(ambassador);
        link.setPublicToken("tok123");
        link.setStatus(ReferralLinkStatus.ACTIVE);

        when(referralRepository.findByReferralCodeAndCompanyId("tok123", 5L)).thenReturn(Optional.empty());
        when(referralLinkRepository.findDetailedByPublicToken("tok123")).thenReturn(Optional.of(link));
        when(platformUserService.findOrCreate(company, "ext-1", "referee@example.com", "Referee")).thenReturn(referee);
        when(referralRepository.findByReferralLinkEntityIdAndCustomerUserId(200L, 40L)).thenReturn(Optional.empty());
        when(referralLinkUrlService.resolveReferralUrl(link)).thenReturn("https://company.example.com/signup?ref=tok123");
        when(referralCodeGenerator.generateUniqueCode()).thenReturn("NEWCODE1");
        when(referralRepository.save(any(Referral.class))).thenAnswer(inv -> {
            Referral r = inv.getArgument(0);
            if (r.getId() == null) {
                r.setId(300L);
            }
            return r;
        });
        when(conversionRepository.existsByReferralIdAndRefereeUserId(300L, 40L)).thenReturn(false);
        when(conversionRepository.save(any(Conversion.class))).thenAnswer(inv -> {
            Conversion c = inv.getArgument(0);
            if (c.getId() == null) {
                c.setId(102L);
            }
            return c;
        });
        when(rewardService.issueRewards(any(Conversion.class))).thenReturn(new RewardResult(null, new Reward(), null));
        when(revenueEventService.recordConversionQualifyingEvent(any(), any())).thenReturn(new AmbassadorReward());

        ConversionService.ConversionWithRewards result = conversionService.completeConversion(request("tok123"));

        assertEquals(300L, result.getConversion().getReferral().getId());
        // Saved twice: once on creation (createReferralFromLink), once more when the ambassador-driven
        // status flip (REGISTERED -> CONVERTED) is applied - both capture the same mutable instance.
        ArgumentCaptor<Referral> savedReferralCaptor = ArgumentCaptor.forClass(Referral.class);
        verify(referralRepository, times(2)).save(savedReferralCaptor.capture());
        assertEquals("NEWCODE1", savedReferralCaptor.getValue().getReferralCode());
        assertEquals(ambassador, savedReferralCaptor.getValue().getAmbassadorUser());
        assertEquals(ReferralStatus.CONVERTED, savedReferralCaptor.getValue().getStatus());
    }

    @Test
    void shouldReuseExistingReferralOnRetriedConversionForSamePublicTokenAndReferee() {
        DashboardUser ambassador = new DashboardUser();
        ambassador.setId(77L);

        ReferralLink link = new ReferralLink();
        link.setId(200L);
        link.setCompany(company);
        link.setCampaign(campaign);
        link.setAmbassadorUser(ambassador);
        link.setPublicToken("tok123");
        link.setStatus(ReferralLinkStatus.ACTIVE);

        Referral existingReferral = new Referral();
        existingReferral.setId(300L);
        existingReferral.setCampaign(campaign);
        existingReferral.setReferrerUser(null);
        existingReferral.setAmbassadorUser(ambassador);
        existingReferral.setStatus(ReferralStatus.CONVERTED);

        when(referralRepository.findByReferralCodeAndCompanyId("tok123", 5L)).thenReturn(Optional.empty());
        when(referralLinkRepository.findDetailedByPublicToken("tok123")).thenReturn(Optional.of(link));
        when(platformUserService.findOrCreate(company, "ext-1", "referee@example.com", "Referee")).thenReturn(referee);
        when(referralRepository.findByReferralLinkEntityIdAndCustomerUserId(200L, 40L)).thenReturn(Optional.of(existingReferral));
        // The first call already converted this referral - the retried call must hit the
        // duplicate-conversion guard rather than silently creating a second Referral.
        when(conversionRepository.existsByReferralIdAndRefereeUserId(300L, 40L)).thenReturn(true);

        assertThrows(BadRequestException.class, () -> conversionService.completeConversion(request("tok123")));

        verify(referralRepository, never()).save(any(Referral.class));
        verify(referralCodeGenerator, never()).generateUniqueCode();
    }

    @Test
    void shouldReturn404ForReferralLinkTokenWhenCampaignIsNotInDirectToLandingPageMode() {
        // Even a real, active, same-company ReferralLink must not be convertible by publicToken
        // alone unless its campaign actually opted into direct-to-landing-page mode - otherwise a
        // company could fabricate a conversion for a link that only ever supports the default
        // /r/{token} -> /refer/{token} lead-capture flow.
        Campaign defaultModeCampaign = new Campaign();
        defaultModeCampaign.setId(4L);
        defaultModeCampaign.setCompany(company);
        defaultModeCampaign.setStatus(CampaignStatus.ACTIVE);
        defaultModeCampaign.setStartDate(LocalDateTime.now().minusDays(1));
        defaultModeCampaign.setEndDate(LocalDateTime.now().plusDays(30));
        defaultModeCampaign.setConversionEventName("booking_completed");
        defaultModeCampaign.setDirectToLandingPageEnabled(false);

        ReferralLink link = new ReferralLink();
        link.setId(200L);
        link.setCompany(company);
        link.setCampaign(defaultModeCampaign);
        link.setPublicToken("tok123");
        link.setStatus(ReferralLinkStatus.ACTIVE);

        when(referralRepository.findByReferralCodeAndCompanyId("tok123", 5L)).thenReturn(Optional.empty());
        when(referralLinkRepository.findDetailedByPublicToken("tok123")).thenReturn(Optional.of(link));

        assertThrows(NotFoundException.class, () -> conversionService.completeConversion(request("tok123")));

        verifyNoInteractions(platformUserService, rewardService, revenueEventService);
    }

    @Test
    void shouldReturn404WhenNeitherReferralCodeNorPublicTokenResolve() {
        when(referralRepository.findByReferralCodeAndCompanyId("unknown", 5L)).thenReturn(Optional.empty());
        when(referralLinkRepository.findDetailedByPublicToken("unknown")).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> conversionService.completeConversion(request("unknown")));

        verifyNoInteractions(platformUserService, rewardService, revenueEventService);
    }

    @Test
    void shouldReturn404ForReferralLinkBelongingToAnotherCompany() {
        Company otherCompany = new Company();
        otherCompany.setId(999L);

        ReferralLink link = new ReferralLink();
        link.setId(200L);
        link.setCompany(otherCompany);
        link.setPublicToken("tok123");
        link.setStatus(ReferralLinkStatus.ACTIVE);

        when(referralRepository.findByReferralCodeAndCompanyId("tok123", 5L)).thenReturn(Optional.empty());
        when(referralLinkRepository.findDetailedByPublicToken("tok123")).thenReturn(Optional.of(link));

        assertThrows(NotFoundException.class, () -> conversionService.completeConversion(request("tok123")));

        verifyNoInteractions(platformUserService);
    }
}
