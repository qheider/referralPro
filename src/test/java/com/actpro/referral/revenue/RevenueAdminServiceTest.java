package com.actpro.referral.revenue;

import com.actpro.referral.auth.DashboardUser;
import com.actpro.referral.campaign.Campaign;
import com.actpro.referral.campaign.CampaignRepository;
import com.actpro.referral.common.exception.BadRequestException;
import com.actpro.referral.common.exception.NotFoundException;
import com.actpro.referral.company.Company;
import com.actpro.referral.referral.Referral;
import com.actpro.referral.revenue.dto.AmbassadorRewardResponse;
import com.actpro.referral.security.CurrentUserService;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Limit;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RevenueAdminServiceTest {

    @Mock
    private AmbassadorRewardRepository ambassadorRewardRepository;

    @Mock
    private CampaignRepository campaignRepository;

    @Mock
    private CurrentUserService currentUserService;

    @Mock
    private EntityManager entityManager;

    private RevenueAdminService service;

    private AmbassadorReward reward;

    @BeforeEach
    void setUp() {
        service = new RevenueAdminService(ambassadorRewardRepository, campaignRepository, currentUserService, entityManager);

        lenient().when(currentUserService.getCurrentCompanyId()).thenReturn(7L);

        Company company = new Company();
        company.setId(7L);
        Campaign campaign = new Campaign();
        campaign.setId(11L);
        campaign.setName("Summer Promo");
        campaign.setCompany(company);
        Referral referral = new Referral();
        referral.setId(21L);
        referral.setReferralCode("REF-21");
        DashboardUser ambassador = new DashboardUser();
        ambassador.setId(31L);
        ambassador.setUsername("amb1");
        RevenueEvent revenueEvent = new RevenueEvent();
        revenueEvent.setId(41L);
        revenueEvent.setQualifyingStatus("COMPLETED");
        revenueEvent.setStatus(RevenueEventStatus.RECORDED);

        reward = new AmbassadorReward();
        reward.setId(51L);
        reward.setCompany(company);
        reward.setCampaign(campaign);
        reward.setReferral(referral);
        reward.setAmbassadorUser(ambassador);
        reward.setRevenueEvent(revenueEvent);
        reward.setRewardValue(new BigDecimal("25.00"));
        reward.setStatus(AmbassadorRewardStatus.ELIGIBLE);

        lenient().when(ambassadorRewardRepository.findByIdAndCompanyId(51L, 7L)).thenReturn(Optional.of(reward));
        lenient().when(ambassadorRewardRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
    }

    @Test
    void shouldApproveAnEligibleReward() {
        AmbassadorRewardResponse response = service.approve(51L);

        assertEquals(AmbassadorRewardStatus.APPROVED, response.status());
        assertNotNull(response.approvedAt());
    }

    @Test
    void shouldRejectApprovingANonEligibleReward() {
        reward.setStatus(AmbassadorRewardStatus.PENDING);

        assertThrows(BadRequestException.class, () -> service.approve(51L));
        verify(ambassadorRewardRepository, never()).save(any());
    }

    @Test
    void shouldMarkAnApprovedRewardPaid() {
        reward.setStatus(AmbassadorRewardStatus.APPROVED);

        AmbassadorRewardResponse response = service.markPaid(51L);

        assertEquals(AmbassadorRewardStatus.PAID, response.status());
        assertNotNull(response.paidAt());
    }

    @Test
    void shouldRejectMarkingANonApprovedRewardPaid() {
        assertThrows(BadRequestException.class, () -> service.markPaid(51L));
    }

    @Test
    void shouldRejectAnEligibleReward() {
        AmbassadorRewardResponse response = service.reject(51L, "Fraudulent booking");

        assertEquals(AmbassadorRewardStatus.REJECTED, response.status());
        assertEquals("Fraudulent booking", response.rejectionReason());
    }

    @Test
    void shouldNotAllowRejectingAPaidReward() {
        reward.setStatus(AmbassadorRewardStatus.PAID);

        assertThrows(BadRequestException.class, () -> service.reject(51L, "too late"));
    }

    @Test
    void shouldThrowNotFoundForAnotherCompanysReward() {
        when(ambassadorRewardRepository.findByIdAndCompanyId(999L, 7L)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> service.getReward(999L));
    }

    @Test
    void shouldRouteListRewardsByCampaignAndStatus() {
        when(ambassadorRewardRepository.findByCompanyIdAndCampaignIdAndStatusOrderByCreatedAtDesc(
                eq(7L), eq(11L), eq(AmbassadorRewardStatus.ELIGIBLE), any(Limit.class))).thenReturn(java.util.List.of(reward));

        var results = service.listRewards(11L, AmbassadorRewardStatus.ELIGIBLE, 50);

        assertEquals(1, results.size());
        verify(ambassadorRewardRepository).findByCompanyIdAndCampaignIdAndStatusOrderByCreatedAtDesc(
                eq(7L), eq(11L), eq(AmbassadorRewardStatus.ELIGIBLE), any(Limit.class));
    }
}
