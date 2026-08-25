package com.actpro.referral.click;

import com.actpro.referral.auth.DashboardUser;
import com.actpro.referral.campaign.Campaign;
import com.actpro.referral.common.exception.NotFoundException;
import com.actpro.referral.company.Company;
import com.actpro.referral.referral.Referral;
import com.actpro.referral.referral.ReferralLink;
import com.actpro.referral.referral.ReferralLinkRepository;
import com.actpro.referral.referral.ReferralLinkStatus;
import com.actpro.referral.referral.ReferralRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReferralClickServiceTest {

    @Mock
    private ReferralClickRepository referralClickRepository;

    @Mock
    private ReferralLinkRepository referralLinkRepository;

    @Mock
    private ReferralRepository referralRepository;

    @InjectMocks
    private ReferralClickService referralClickService;

    private Company company;
    private Campaign campaign;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(referralClickService, "frontendUrl", "https://app.example.com");

        company = new Company();
        company.setId(5L);
        company.setName("Acme");

        campaign = new Campaign();
        campaign.setId(8L);
        campaign.setCompany(company);
        campaign.setLandingPageUrl("https://campaign.example.com");
    }

    @Test
    void shouldRecordClickAndRedirectToInternalRegistrationPageForAmbassadorLinkToken() {
        DashboardUser ambassadorUser = new DashboardUser();
        ambassadorUser.setId(21L);

        ReferralLink link = new ReferralLink();
        link.setId(77L);
        link.setCompany(company);
        link.setCampaign(campaign);
        link.setAmbassadorUser(ambassadorUser);
        link.setPublicToken("AbcDef1234567890");
        link.setStatus(ReferralLinkStatus.ACTIVE);

        when(referralLinkRepository.findDetailedByPublicToken("AbcDef1234567890")).thenReturn(Optional.of(link));

        String redirectUrl = referralClickService.resolveAndRecordClick(
                "AbcDef1234567890", "203.0.113.5", "test-agent", "https://source.example.com", "session-1"
        );

        assertEquals("https://app.example.com/refer/AbcDef1234567890?s=session-1", redirectUrl);

        ArgumentCaptor<ReferralClick> captor = ArgumentCaptor.forClass(ReferralClick.class);
        verify(referralClickRepository).save(captor.capture());
        ReferralClick click = captor.getValue();
        assertEquals(link, click.getReferralLink());
        assertEquals(company, click.getCompany());
        assertEquals(campaign, click.getCampaign());
        assertEquals(ambassadorUser, click.getAmbassadorUser());
        assertEquals("session-1", click.getSessionId());
        assertEquals("203.0.113.5", click.getIpAddress());
        assertNotNull(click.getIpHash());
        assertEquals("test-agent", click.getUserAgent());
        assertEquals("https://source.example.com", click.getReferrerUrl());
        assertNull(click.getReferral());

        verify(referralLinkRepository).incrementClickCount(77L);
        verify(referralRepository, never()).findByReferralCodeWithCampaign(any());
    }

    @Test
    void shouldOmitSessionQueryParamWhenNoSessionId() {
        ReferralLink link = new ReferralLink();
        link.setId(77L);
        link.setCompany(company);
        link.setCampaign(campaign);
        link.setPublicToken("AbcDef1234567890");
        link.setStatus(ReferralLinkStatus.ACTIVE);

        when(referralLinkRepository.findDetailedByPublicToken("AbcDef1234567890")).thenReturn(Optional.of(link));

        String redirectUrl = referralClickService.resolveAndRecordClick(
                "AbcDef1234567890", "203.0.113.5", "test-agent", null, null
        );

        assertEquals("https://app.example.com/refer/AbcDef1234567890", redirectUrl);
    }

    @Test
    void shouldRedirectToInternalRegistrationPageEvenWhenLinkHasDestinationUrl() {
        // destinationUrl is only used later, as a post-registration forward from
        // ReferralLeadService once the lead is captured - see that class's toResponse. The
        // click-time redirect always goes to ReferralPro's own page so registration is tracked
        // independently of the ambassador's own site.
        ReferralLink link = new ReferralLink();
        link.setId(77L);
        link.setCompany(company);
        link.setCampaign(campaign);
        link.setPublicToken("AbcDef1234567890");
        link.setDestinationUrl("https://custom.example.com/landing");
        link.setStatus(ReferralLinkStatus.ACTIVE);

        when(referralLinkRepository.findDetailedByPublicToken("AbcDef1234567890")).thenReturn(Optional.of(link));

        String redirectUrl = referralClickService.resolveAndRecordClick(
                "AbcDef1234567890", "203.0.113.5", "test-agent", null, "session-1"
        );

        assertEquals("https://app.example.com/refer/AbcDef1234567890?s=session-1", redirectUrl);
    }

    @Test
    void shouldRecordNullIpHashWhenIpAddressBlank() {
        ReferralLink link = new ReferralLink();
        link.setId(77L);
        link.setCompany(company);
        link.setCampaign(campaign);
        link.setPublicToken("AbcDef1234567890");
        link.setStatus(ReferralLinkStatus.ACTIVE);

        when(referralLinkRepository.findDetailedByPublicToken("AbcDef1234567890")).thenReturn(Optional.of(link));

        referralClickService.resolveAndRecordClick("AbcDef1234567890", null, "test-agent", null, "session-1");

        ArgumentCaptor<ReferralClick> captor = ArgumentCaptor.forClass(ReferralClick.class);
        verify(referralClickRepository).save(captor.capture());
        assertNull(captor.getValue().getIpAddress());
        assertNull(captor.getValue().getIpHash());
    }

    @Test
    void shouldRejectInactiveAmbassadorLink() {
        ReferralLink link = new ReferralLink();
        link.setId(77L);
        link.setStatus(ReferralLinkStatus.DISABLED);

        when(referralLinkRepository.findDetailedByPublicToken("AbcDef1234567890")).thenReturn(Optional.of(link));

        assertThrows(
                NotFoundException.class,
                () -> referralClickService.resolveAndRecordClick("AbcDef1234567890", "1.1.1.1", "ua", null, "session-1")
        );

        verify(referralClickRepository, never()).save(any());
    }

    @Test
    void shouldRejectLinkWithExpiredStatus() {
        ReferralLink link = new ReferralLink();
        link.setId(77L);
        link.setStatus(ReferralLinkStatus.EXPIRED);

        when(referralLinkRepository.findDetailedByPublicToken("AbcDef1234567890")).thenReturn(Optional.of(link));

        assertThrows(
                NotFoundException.class,
                () -> referralClickService.resolveAndRecordClick("AbcDef1234567890", "1.1.1.1", "ua", null, "session-1")
        );

        verify(referralClickRepository, never()).save(any());
    }

    @Test
    void shouldRejectExpiredAmbassadorLink() {
        ReferralLink link = new ReferralLink();
        link.setId(77L);
        link.setStatus(ReferralLinkStatus.ACTIVE);
        link.setExpiresAt(LocalDateTime.now().minusDays(1));

        when(referralLinkRepository.findDetailedByPublicToken("AbcDef1234567890")).thenReturn(Optional.of(link));

        assertThrows(
                NotFoundException.class,
                () -> referralClickService.resolveAndRecordClick("AbcDef1234567890", "1.1.1.1", "ua", null, "session-1")
        );

        verify(referralClickRepository, never()).save(any());
    }

    @Test
    void shouldFallBackToLegacyReferralCodeWhenNoLinkMatches() {
        Referral referral = new Referral();
        referral.setId(9L);
        referral.setCompany(company);
        referral.setCampaign(campaign);
        referral.setReferralCode("LUP3X9KM");

        when(referralLinkRepository.findDetailedByPublicToken("LUP3X9KM")).thenReturn(Optional.empty());
        when(referralRepository.findByReferralCodeWithCampaign("LUP3X9KM")).thenReturn(Optional.of(referral));

        String redirectUrl = referralClickService.resolveAndRecordClick(
                "LUP3X9KM", "203.0.113.5", "test-agent", null, "session-2"
        );

        assertEquals("https://campaign.example.com?ref=LUP3X9KM", redirectUrl);

        ArgumentCaptor<ReferralClick> captor = ArgumentCaptor.forClass(ReferralClick.class);
        verify(referralClickRepository).save(captor.capture());
        assertEquals(referral, captor.getValue().getReferral());
        assertEquals("session-2", captor.getValue().getSessionId());

        verify(referralLinkRepository, never()).incrementClickCount(any());
    }

    @Test
    void shouldThrowNotFoundWhenNeitherLinkNorReferralMatches() {
        when(referralLinkRepository.findDetailedByPublicToken("UNKNOWN")).thenReturn(Optional.empty());
        when(referralRepository.findByReferralCodeWithCampaign("UNKNOWN")).thenReturn(Optional.empty());

        assertThrows(
                NotFoundException.class,
                () -> referralClickService.resolveAndRecordClick("UNKNOWN", "1.1.1.1", "ua", null, "session-3")
        );

        verify(referralClickRepository, never()).save(any());
    }
}
