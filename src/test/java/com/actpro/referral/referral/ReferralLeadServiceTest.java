package com.actpro.referral.referral;

import com.actpro.referral.auth.DashboardUser;
import com.actpro.referral.campaign.Campaign;
import com.actpro.referral.click.ReferralClick;
import com.actpro.referral.click.ReferralClickRepository;
import com.actpro.referral.common.exception.BadRequestException;
import com.actpro.referral.common.exception.NotFoundException;
import com.actpro.referral.company.Company;
import com.actpro.referral.outbox.OutboxEventPublisher;
import com.actpro.referral.referral.dto.SubmitReferralLeadRequest;
import com.actpro.referral.referral.dto.SubmitReferralLeadResponse;
import com.actpro.referral.user.PlatformUser;
import com.actpro.referral.user.PlatformUserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReferralLeadServiceTest {

    @Mock
    private ReferralLinkRepository referralLinkRepository;

    @Mock
    private ReferralRepository referralRepository;

    @Mock
    private ReferralClickRepository referralClickRepository;

    @Mock
    private PlatformUserService platformUserService;

    @Mock
    private ReferralCodeGenerator referralCodeGenerator;

    @Mock
    private OutboxEventPublisher outboxEventPublisher;

    @InjectMocks
    private ReferralLeadService referralLeadService;

    private Company company;
    private Campaign campaign;
    private ReferralLink link;
    private DashboardUser ambassadorUser;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(referralLeadService, "baseUrl", "https://app.example.com");

        company = new Company();
        company.setId(5L);

        campaign = new Campaign();
        campaign.setId(8L);
        campaign.setCompany(company);

        ambassadorUser = new DashboardUser();
        ambassadorUser.setId(21L);

        link = new ReferralLink();
        link.setId(77L);
        link.setCompany(company);
        link.setCampaign(campaign);
        link.setAmbassadorUser(ambassadorUser);
        link.setPublicToken("AbcDef1234567890");
        link.setStatus(ReferralLinkStatus.ACTIVE);
    }

    private SubmitReferralLeadRequest validRequest() {
        return new SubmitReferralLeadRequest("Jamie Lee", "jamie@example.com");
    }

    @Test
    void shouldCreateReferralAndPublishOutboxEvent() {
        when(referralLinkRepository.findDetailedByPublicToken("AbcDef1234567890")).thenReturn(Optional.of(link));
        when(referralRepository.existsByReferralLinkEntityIdAndCustomerUserEmailAndStatusNotIn(eq(77L), eq("jamie@example.com"), any()))
                .thenReturn(false);
        PlatformUser customer = new PlatformUser();
        customer.setId(31L);
        customer.setEmail("jamie@example.com");
        when(platformUserService.findOrCreate(eq(company), anyString(), eq("jamie@example.com"), eq("Jamie Lee")))
                .thenReturn(customer);
        when(referralCodeGenerator.generateUniqueCode()).thenReturn("LEAD1234");
        when(referralRepository.save(any(Referral.class))).thenAnswer(invocation -> {
            Referral referral = invocation.getArgument(0);
            referral.setId(99L);
            return referral;
        });
        when(referralClickRepository.findByReferralLinkIdAndSessionIdAndReferralIsNull(77L, "session-1")).thenReturn(List.of());

        SubmitReferralLeadResponse response = referralLeadService.submitLead("AbcDef1234567890", "session-1", validRequest());

        ArgumentCaptor<Referral> captor = ArgumentCaptor.forClass(Referral.class);
        verify(referralRepository).save(captor.capture());
        Referral saved = captor.getValue();

        assertEquals(company, saved.getCompany());
        assertEquals(campaign, saved.getCampaign());
        assertEquals(ambassadorUser, saved.getAmbassadorUser());
        assertEquals(link, saved.getReferralLinkEntity());
        assertEquals(customer, saved.getCustomerUser());
        assertEquals("LEAD1234", saved.getReferralCode());
        assertEquals(ReferralStatus.REGISTERED, saved.getStatus());
        assertEquals("session-1", saved.getAttributionSessionId());
        assertEquals("https://app.example.com/r/AbcDef1234567890", saved.getReferralLink());

        assertEquals("LEAD1234", response.referralCode());
        assertEquals(ReferralStatus.REGISTERED, response.status());
        assertNull(response.redirectUrl());

        verify(outboxEventPublisher).publish(eq(company), eq("REFERRAL"), eq(99L), eq("referral.lead_registered"), any());
    }

    @Test
    void shouldReturnRedirectUrlWhenLinkHasDestinationUrl() {
        link.setDestinationUrl("https://custom.example.com/landing");

        when(referralLinkRepository.findDetailedByPublicToken("AbcDef1234567890")).thenReturn(Optional.of(link));
        when(referralRepository.existsByReferralLinkEntityIdAndCustomerUserEmailAndStatusNotIn(eq(77L), eq("jamie@example.com"), any()))
                .thenReturn(false);
        PlatformUser customer = new PlatformUser();
        customer.setId(31L);
        customer.setEmail("jamie@example.com");
        when(platformUserService.findOrCreate(eq(company), anyString(), eq("jamie@example.com"), eq("Jamie Lee")))
                .thenReturn(customer);
        when(referralCodeGenerator.generateUniqueCode()).thenReturn("LEAD1234");
        when(referralRepository.save(any(Referral.class))).thenAnswer(invocation -> {
            Referral referral = invocation.getArgument(0);
            referral.setId(99L);
            return referral;
        });
        when(referralClickRepository.findByReferralLinkIdAndSessionIdAndReferralIsNull(77L, "session-1")).thenReturn(List.of());

        SubmitReferralLeadResponse response = referralLeadService.submitLead("AbcDef1234567890", "session-1", validRequest());

        assertEquals("https://custom.example.com/landing?ref=LEAD1234", response.redirectUrl());
    }

    @Test
    void shouldIncludeRedirectUrlOnIdempotentExistingReferralWhenLinkHasDestinationUrl() {
        link.setDestinationUrl("https://custom.example.com/landing");

        Referral existing = new Referral();
        existing.setId(50L);
        existing.setReferralCode("EXISTING1");
        existing.setStatus(ReferralStatus.REGISTERED);
        existing.setRegisteredAt(LocalDateTime.now());

        when(referralLinkRepository.findDetailedByPublicToken("AbcDef1234567890")).thenReturn(Optional.of(link));
        when(referralRepository.findByReferralLinkEntityIdAndAttributionSessionId(77L, "session-1")).thenReturn(Optional.of(existing));

        SubmitReferralLeadResponse response = referralLeadService.submitLead("AbcDef1234567890", "session-1", validRequest());

        assertEquals("https://custom.example.com/landing?ref=EXISTING1", response.redirectUrl());
    }

    @Test
    void shouldReturnExistingReferralForSameSessionIdempotently() {
        Referral existing = new Referral();
        existing.setId(50L);
        existing.setReferralCode("EXISTING1");
        existing.setStatus(ReferralStatus.REGISTERED);
        existing.setRegisteredAt(LocalDateTime.now());

        when(referralLinkRepository.findDetailedByPublicToken("AbcDef1234567890")).thenReturn(Optional.of(link));
        when(referralRepository.findByReferralLinkEntityIdAndAttributionSessionId(77L, "session-1")).thenReturn(Optional.of(existing));

        SubmitReferralLeadResponse response = referralLeadService.submitLead("AbcDef1234567890", "session-1", validRequest());

        assertEquals("EXISTING1", response.referralCode());
        verify(referralRepository, never()).save(any());
        verify(platformUserService, never()).findOrCreate(any(), any(), any(), any());
        verify(outboxEventPublisher, never()).publish(any(), any(), any(), any(), any());
    }

    @Test
    void shouldRejectWhenLinkNotFound() {
        when(referralLinkRepository.findDetailedByPublicToken("UNKNOWN")).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> referralLeadService.submitLead("UNKNOWN", "session-1", validRequest()));
        verify(referralRepository, never()).save(any());
    }

    @Test
    void shouldRejectWhenLinkInactive() {
        link.setStatus(ReferralLinkStatus.DISABLED);
        when(referralLinkRepository.findDetailedByPublicToken("AbcDef1234567890")).thenReturn(Optional.of(link));

        assertThrows(NotFoundException.class, () -> referralLeadService.submitLead("AbcDef1234567890", "session-1", validRequest()));
        verify(referralRepository, never()).save(any());
    }

    @Test
    void shouldRejectWhenLinkExpired() {
        link.setExpiresAt(LocalDateTime.now().minusDays(1));
        when(referralLinkRepository.findDetailedByPublicToken("AbcDef1234567890")).thenReturn(Optional.of(link));

        assertThrows(NotFoundException.class, () -> referralLeadService.submitLead("AbcDef1234567890", "session-1", validRequest()));
        verify(referralRepository, never()).save(any());
    }

    @Test
    void shouldRejectDuplicateEmailSubmissionInDifferentSession() {
        when(referralLinkRepository.findDetailedByPublicToken("AbcDef1234567890")).thenReturn(Optional.of(link));
        when(referralRepository.findByReferralLinkEntityIdAndAttributionSessionId(77L, "session-2")).thenReturn(Optional.empty());
        when(referralRepository.existsByReferralLinkEntityIdAndCustomerUserEmailAndStatusNotIn(eq(77L), eq("jamie@example.com"), any()))
                .thenReturn(true);

        assertThrows(BadRequestException.class, () -> referralLeadService.submitLead("AbcDef1234567890", "session-2", validRequest()));
        verify(referralRepository, never()).save(any());
    }

    @Test
    void shouldBackfillClicksMissingReferralForSameSession() {
        when(referralLinkRepository.findDetailedByPublicToken("AbcDef1234567890")).thenReturn(Optional.of(link));
        when(referralRepository.existsByReferralLinkEntityIdAndCustomerUserEmailAndStatusNotIn(eq(77L), eq("jamie@example.com"), any()))
                .thenReturn(false);
        PlatformUser customer = new PlatformUser();
        customer.setId(31L);
        customer.setEmail("jamie@example.com");
        when(platformUserService.findOrCreate(any(), anyString(), anyString(), anyString())).thenReturn(customer);
        when(referralCodeGenerator.generateUniqueCode()).thenReturn("LEAD1234");
        when(referralRepository.save(any(Referral.class))).thenAnswer(invocation -> {
            Referral referral = invocation.getArgument(0);
            referral.setId(99L);
            return referral;
        });

        ReferralClick click = new ReferralClick();
        when(referralClickRepository.findByReferralLinkIdAndSessionIdAndReferralIsNull(77L, "session-1")).thenReturn(List.of(click));

        referralLeadService.submitLead("AbcDef1234567890", "session-1", validRequest());

        assertEquals(99L, click.getReferral().getId());
    }

    @Test
    void shouldSkipSessionLookupsWhenSessionIdIsNull() {
        when(referralLinkRepository.findDetailedByPublicToken("AbcDef1234567890")).thenReturn(Optional.of(link));
        when(referralRepository.existsByReferralLinkEntityIdAndCustomerUserEmailAndStatusNotIn(eq(77L), eq("jamie@example.com"), any()))
                .thenReturn(false);
        PlatformUser customer = new PlatformUser();
        customer.setId(31L);
        customer.setEmail("jamie@example.com");
        when(platformUserService.findOrCreate(any(), anyString(), anyString(), anyString())).thenReturn(customer);
        when(referralCodeGenerator.generateUniqueCode()).thenReturn("LEAD1234");
        when(referralRepository.save(any(Referral.class))).thenAnswer(invocation -> {
            Referral referral = invocation.getArgument(0);
            referral.setId(99L);
            return referral;
        });

        SubmitReferralLeadResponse response = referralLeadService.submitLead("AbcDef1234567890", null, validRequest());

        assertEquals("LEAD1234", response.referralCode());
        verify(referralRepository, never()).findByReferralLinkEntityIdAndAttributionSessionId(any(), any());
        verify(referralClickRepository, never()).findByReferralLinkIdAndSessionIdAndReferralIsNull(any(), any());
    }
}
