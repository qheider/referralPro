package com.actpro.referral.ambassador;

import com.actpro.referral.ambassador.dto.AmbassadorEarningsHistoryResponse;
import com.actpro.referral.ambassador.dto.AmbassadorEarningsSummaryResponse;
import com.actpro.referral.auth.DashboardUser;
import com.actpro.referral.campaign.Campaign;
import com.actpro.referral.campaign.RewardType;
import com.actpro.referral.click.ReferralClickRepository;
import com.actpro.referral.company.Company;
import com.actpro.referral.referral.Referral;
import com.actpro.referral.referral.ReferralLinkRepository;
import com.actpro.referral.referral.ReferralLinkUrlService;
import com.actpro.referral.referral.ReferralRepository;
import com.actpro.referral.revenue.AmbassadorReward;
import com.actpro.referral.revenue.AmbassadorRewardRepository;
import com.actpro.referral.revenue.AmbassadorRewardStatus;
import com.actpro.referral.security.CurrentUserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AmbassadorPortalServiceEarningsTest {

    @Mock
    private CurrentUserService currentUserService;
    @Mock
    private AmbassadorProfileRepository ambassadorProfileRepository;
    @Mock
    private CampaignAmbassadorAssignmentRepository assignmentRepository;
    @Mock
    private ReferralLinkRepository referralLinkRepository;
    @Mock
    private ReferralRepository referralRepository;
    @Mock
    private ReferralClickRepository referralClickRepository;
    @Mock
    private AmbassadorRewardRepository ambassadorRewardRepository;
    @Mock
    private ReferralLinkUrlService referralLinkUrlService;

    private AmbassadorPortalService service;

    private AmbassadorProfile profile;

    @BeforeEach
    void setUp() {
        service = new AmbassadorPortalService(
                currentUserService, ambassadorProfileRepository, assignmentRepository,
                referralLinkRepository, referralRepository, referralClickRepository, ambassadorRewardRepository,
                referralLinkUrlService);

        Company company = new Company();
        company.setId(1L);
        DashboardUser user = new DashboardUser();
        user.setId(2L);
        user.setUsername("amb1");

        profile = new AmbassadorProfile();
        profile.setId(3L);
        profile.setCompany(company);
        profile.setUser(user);

        when(currentUserService.getCurrentAmbassadorProfile()).thenReturn(profile);
    }

    private AmbassadorReward reward(AmbassadorRewardStatus status, String value, String currency) {
        Campaign campaign = new Campaign();
        campaign.setId(10L);
        campaign.setName("Campaign");
        campaign.setRewardType(RewardType.CREDIT);
        Referral referral = new Referral();
        referral.setId(20L);
        referral.setReferralCode("REF-20");

        AmbassadorReward reward = new AmbassadorReward();
        reward.setCampaign(campaign);
        reward.setReferral(referral);
        reward.setStatus(status);
        reward.setRewardValue(new BigDecimal(value));
        reward.setCurrency(currency);
        reward.setRewardType(RewardType.CREDIT);
        return reward;
    }

    @Test
    void shouldSumEarningsByStatusBucket() {
        List<AmbassadorReward> rewards = List.of(
                reward(AmbassadorRewardStatus.PAID, "10.00", "USD"),
                reward(AmbassadorRewardStatus.PAID, "5.00", "USD"),
                reward(AmbassadorRewardStatus.APPROVED, "7.00", "USD"),
                reward(AmbassadorRewardStatus.ELIGIBLE, "3.00", "USD"),
                reward(AmbassadorRewardStatus.PENDING, "2.00", "USD"),
                reward(AmbassadorRewardStatus.REJECTED, "1.00", "USD"),
                reward(AmbassadorRewardStatus.REVERSED, "4.00", "USD")
        );
        when(ambassadorRewardRepository.findByAmbassadorUserIdAndCompanyId(2L, 1L)).thenReturn(rewards);

        AmbassadorEarningsSummaryResponse summary = service.getEarningsSummary();

        assertEquals(new BigDecimal("15.00"), summary.totalPaid());
        assertEquals(new BigDecimal("7.00"), summary.totalApproved());
        assertEquals(new BigDecimal("5.00"), summary.totalPendingOrEligible());
        assertEquals(new BigDecimal("5.00"), summary.totalRejectedOrReversed());
        assertEquals(7, summary.rewardCount());
        assertEquals("USD", summary.currency());
    }

    @Test
    void shouldReturnZeroSummaryWhenNoRewardsExist() {
        when(ambassadorRewardRepository.findByAmbassadorUserIdAndCompanyId(2L, 1L)).thenReturn(List.of());

        AmbassadorEarningsSummaryResponse summary = service.getEarningsSummary();

        assertEquals(BigDecimal.ZERO, summary.totalPaid());
        assertEquals(0, summary.rewardCount());
    }

    @Test
    void shouldMapPaginatedEarningsHistory() {
        AmbassadorReward reward = reward(AmbassadorRewardStatus.PAID, "10.00", "USD");
        Page<AmbassadorReward> page = new PageImpl<>(List.of(reward));
        when(ambassadorRewardRepository.findByAmbassadorUserIdAndCompanyId(eq(2L), eq(1L), any(Pageable.class))).thenReturn(page);

        AmbassadorEarningsHistoryResponse history = service.listEarnings(0, 20);

        assertEquals(1, history.rewards().size());
        assertEquals("REF-20", history.rewards().get(0).referralCode());
        assertEquals(AmbassadorRewardStatus.PAID, history.rewards().get(0).status());
    }
}
