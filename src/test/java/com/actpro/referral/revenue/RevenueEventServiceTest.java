package com.actpro.referral.revenue;

import com.actpro.referral.auth.DashboardUser;
import com.actpro.referral.campaign.Campaign;
import com.actpro.referral.campaign.RewardType;
import com.actpro.referral.company.Company;
import com.actpro.referral.company.CompanyIntegration;
import com.actpro.referral.company.CompanyIntegrationRepository;
import com.actpro.referral.referral.Referral;
import com.actpro.referral.referral.ReferralRepository;
import com.actpro.referral.referral.ReferralStatus;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RevenueEventServiceTest {

    @Mock
    private ReferralRepository referralRepository;

    @Mock
    private CompanyIntegrationRepository companyIntegrationRepository;

    @Mock
    private RevenueEventRepository revenueEventRepository;

    @Mock
    private AmbassadorRewardRepository ambassadorRewardRepository;

    private RevenueEventService service;

    private Company company;
    private Campaign campaign;
    private DashboardUser ambassador;
    private Referral referral;

    @BeforeEach
    void setUp() {
        service = new RevenueEventService(
                referralRepository, companyIntegrationRepository, revenueEventRepository,
                ambassadorRewardRepository, new RewardStatusMappingService(new ObjectMapper()));

        company = new Company();
        company.setId(1L);
        company.setPreferredCurrency("USD");

        campaign = new Campaign();
        campaign.setId(10L);
        campaign.setCompany(company);
        campaign.setRewardType(RewardType.CREDIT);
        campaign.setReferrerRewardValue(new BigDecimal("25.00"));

        ambassador = new DashboardUser();
        ambassador.setId(20L);

        referral = new Referral();
        referral.setId(30L);
        referral.setCompany(company);
        referral.setCampaign(campaign);
        referral.setAmbassadorUser(ambassador);
        referral.setStatus(ReferralStatus.COMPLETED);

        // lenient(): not every test's referral reaches the branch that needs each of these -
        // same convention as CompanyIntegrationServiceTest's shared-setUp save() stub.
        org.mockito.Mockito.lenient().when(referralRepository.findById(30L)).thenReturn(Optional.of(referral));
        org.mockito.Mockito.lenient().when(companyIntegrationRepository.findByCompanyId(1L)).thenReturn(Optional.empty());
        org.mockito.Mockito.lenient().when(revenueEventRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        org.mockito.Mockito.lenient().when(ambassadorRewardRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
    }

    @Test
    void shouldSkipWhenReferralNotFound() {
        when(referralRepository.findById(999L)).thenReturn(Optional.empty());

        service.applyReferralStatusChange(company, 999L, null, null, LocalDateTime.now());

        verify(revenueEventRepository, never()).save(any());
    }

    @Test
    void shouldSkipLegacyReferralWithNoAmbassador() {
        referral.setAmbassadorUser(null);

        service.applyReferralStatusChange(company, 30L, null, null, LocalDateTime.now());

        verify(revenueEventRepository, never()).findByReferralId(any());
        verify(revenueEventRepository, never()).save(any());
    }

    @Test
    void shouldIgnoreNonQualifyingNonReversingStatus() {
        referral.setStatus(ReferralStatus.BOOKING_STARTED);

        service.applyReferralStatusChange(company, 30L, null, null, LocalDateTime.now());

        verify(revenueEventRepository, never()).save(any());
        verify(ambassadorRewardRepository, never()).save(any());
    }

    @Test
    void shouldRecordRevenueEventAndEligibleRewardWhenNoCurrencyMismatch() {
        when(revenueEventRepository.findByReferralId(30L)).thenReturn(Optional.empty());

        service.applyReferralStatusChange(company, 30L, new BigDecimal("150.00"), "USD", LocalDateTime.now());

        ArgumentCaptor<RevenueEvent> eventCaptor = ArgumentCaptor.forClass(RevenueEvent.class);
        verify(revenueEventRepository).save(eventCaptor.capture());
        RevenueEvent savedEvent = eventCaptor.getValue();
        assertEquals("COMPLETED", savedEvent.getQualifyingStatus());
        assertEquals(new BigDecimal("150.00"), savedEvent.getRevenueAmount());
        assertFalse(savedEvent.isCurrencyMismatch());
        assertEquals(RevenueEventStatus.RECORDED, savedEvent.getStatus());

        ArgumentCaptor<AmbassadorReward> rewardCaptor = ArgumentCaptor.forClass(AmbassadorReward.class);
        verify(ambassadorRewardRepository).save(rewardCaptor.capture());
        AmbassadorReward savedReward = rewardCaptor.getValue();
        assertEquals(RewardType.CREDIT, savedReward.getRewardType());
        assertEquals(new BigDecimal("25.00"), savedReward.getRewardValue());
        assertEquals(AmbassadorRewardStatus.ELIGIBLE, savedReward.getStatus());
        assertNull(savedReward.getHoldReason());
        assertEquals(ambassador, savedReward.getAmbassadorUser());
    }

    @Test
    void shouldHoldRewardPendingOnCurrencyMismatch() {
        when(revenueEventRepository.findByReferralId(30L)).thenReturn(Optional.empty());

        service.applyReferralStatusChange(company, 30L, new BigDecimal("150.00"), "EUR", LocalDateTime.now());

        ArgumentCaptor<RevenueEvent> eventCaptor = ArgumentCaptor.forClass(RevenueEvent.class);
        verify(revenueEventRepository).save(eventCaptor.capture());
        assertTrue(eventCaptor.getValue().isCurrencyMismatch());

        ArgumentCaptor<AmbassadorReward> rewardCaptor = ArgumentCaptor.forClass(AmbassadorReward.class);
        verify(ambassadorRewardRepository).save(rewardCaptor.capture());
        assertEquals(AmbassadorRewardStatus.PENDING, rewardCaptor.getValue().getStatus());
        assertEquals("CURRENCY_MISMATCH", rewardCaptor.getValue().getHoldReason());
        // The payout amount/currency still come from the campaign snapshot, never the mismatched webhook currency.
        assertEquals(new BigDecimal("25.00"), rewardCaptor.getValue().getRewardValue());
        assertEquals("USD", rewardCaptor.getValue().getCurrency());
    }

    @Test
    void shouldNotCreateDuplicateRewardOnRedeliveryOfSameQualifyingEvent() {
        RevenueEvent existing = new RevenueEvent();
        existing.setId(100L);
        existing.setStatus(RevenueEventStatus.RECORDED);
        existing.setQualifyingStatus("COMPLETED");
        when(revenueEventRepository.findByReferralId(30L)).thenReturn(Optional.of(existing));

        service.applyReferralStatusChange(company, 30L, new BigDecimal("150.00"), "USD", LocalDateTime.now());

        verify(revenueEventRepository, times(1)).save(existing);
        verify(ambassadorRewardRepository, never()).save(any());
    }

    @Test
    void shouldUpdateQualifyingStatusOnFurtherProgressWithoutTouchingReward() {
        RevenueEvent existing = new RevenueEvent();
        existing.setId(100L);
        existing.setStatus(RevenueEventStatus.RECORDED);
        existing.setQualifyingStatus("COMPLETED");
        when(revenueEventRepository.findByReferralId(30L)).thenReturn(Optional.of(existing));
        referral.setStatus(ReferralStatus.CONVERTED);

        service.applyReferralStatusChange(company, 30L, null, null, LocalDateTime.now());

        assertEquals("CONVERTED", existing.getQualifyingStatus());
        verify(ambassadorRewardRepository, never()).save(any());
    }

    @Test
    void shouldReverseEligibleRewardOnCancellation() {
        RevenueEvent existing = new RevenueEvent();
        existing.setId(100L);
        existing.setStatus(RevenueEventStatus.RECORDED);
        AmbassadorReward reward = new AmbassadorReward();
        reward.setId(200L);
        reward.setStatus(AmbassadorRewardStatus.ELIGIBLE);
        when(revenueEventRepository.findByReferralId(30L)).thenReturn(Optional.of(existing));
        when(ambassadorRewardRepository.findByRevenueEventId(100L)).thenReturn(Optional.of(reward));
        referral.setStatus(ReferralStatus.CANCELLED);

        service.applyReferralStatusChange(company, 30L, null, null, LocalDateTime.now());

        assertEquals(RevenueEventStatus.REVERSED, existing.getStatus());
        assertEquals(AmbassadorRewardStatus.REVERSED, reward.getStatus());
        verify(ambassadorRewardRepository).save(reward);
    }

    @Test
    void shouldNotReverseAnAlreadyPaidReward() {
        RevenueEvent existing = new RevenueEvent();
        existing.setId(100L);
        existing.setStatus(RevenueEventStatus.RECORDED);
        AmbassadorReward reward = new AmbassadorReward();
        reward.setId(200L);
        reward.setStatus(AmbassadorRewardStatus.PAID);
        when(revenueEventRepository.findByReferralId(30L)).thenReturn(Optional.of(existing));
        when(ambassadorRewardRepository.findByRevenueEventId(100L)).thenReturn(Optional.of(reward));
        referral.setStatus(ReferralStatus.REJECTED);

        service.applyReferralStatusChange(company, 30L, null, null, LocalDateTime.now());

        // The RevenueEvent still reverses (for reporting/reconciliation)...
        assertEquals(RevenueEventStatus.REVERSED, existing.getStatus());
        // ...but a PAID reward is never clawed back automatically.
        assertEquals(AmbassadorRewardStatus.PAID, reward.getStatus());
        verify(ambassadorRewardRepository, never()).save(reward);
    }

    @Test
    void shouldNoOpWhenReversingAndNoRevenueEventWasEverRecorded() {
        when(revenueEventRepository.findByReferralId(30L)).thenReturn(Optional.empty());
        referral.setStatus(ReferralStatus.CANCELLED);

        service.applyReferralStatusChange(company, 30L, null, null, LocalDateTime.now());

        verify(revenueEventRepository, never()).save(any());
        verify(ambassadorRewardRepository, never()).save(any());
    }

    @Test
    void shouldRecordConversionQualifyingEventAndReturnTheCreatedReward() {
        when(revenueEventRepository.findByReferralId(30L)).thenReturn(Optional.empty());
        referral.setStatus(ReferralStatus.CONVERTED);

        AmbassadorReward reward = service.recordConversionQualifyingEvent(referral, LocalDateTime.now());

        assertEquals(AmbassadorRewardStatus.ELIGIBLE, reward.getStatus());
        assertEquals(ambassador, reward.getAmbassadorUser());
        verify(revenueEventRepository).save(any(RevenueEvent.class));
        verify(ambassadorRewardRepository).save(any(AmbassadorReward.class));
    }

    @Test
    void shouldBeIdempotentWhenRecordConversionQualifyingEventIsRetriedForSameReferral() {
        RevenueEvent existing = new RevenueEvent();
        existing.setId(100L);
        existing.setStatus(RevenueEventStatus.RECORDED);
        existing.setQualifyingStatus("CONVERTED");
        AmbassadorReward existingReward = new AmbassadorReward();
        existingReward.setId(200L);
        existingReward.setStatus(AmbassadorRewardStatus.ELIGIBLE);
        when(revenueEventRepository.findByReferralId(30L)).thenReturn(Optional.of(existing));
        when(ambassadorRewardRepository.findByRevenueEventId(100L)).thenReturn(Optional.of(existingReward));
        referral.setStatus(ReferralStatus.CONVERTED);

        AmbassadorReward reward = service.recordConversionQualifyingEvent(referral, LocalDateTime.now());

        assertEquals(200L, reward.getId());
        verify(ambassadorRewardRepository, never()).save(any());
    }

    @Test
    void shouldRejectRecordConversionQualifyingEventForReferralWithNoAmbassadorUser() {
        referral.setAmbassadorUser(null);

        assertThrows(IllegalStateException.class,
                () -> service.recordConversionQualifyingEvent(referral, LocalDateTime.now()));
    }

    @Test
    void shouldBeIdempotentOnRepeatedReversal() {
        RevenueEvent existing = new RevenueEvent();
        existing.setId(100L);
        existing.setStatus(RevenueEventStatus.REVERSED);
        when(revenueEventRepository.findByReferralId(30L)).thenReturn(Optional.of(existing));
        referral.setStatus(ReferralStatus.CANCELLED);

        service.applyReferralStatusChange(company, 30L, null, null, LocalDateTime.now());

        verify(revenueEventRepository, never()).save(any());
        verify(ambassadorRewardRepository, never()).findByRevenueEventId(any());
    }
}
