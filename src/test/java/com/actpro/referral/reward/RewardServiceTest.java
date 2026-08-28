package com.actpro.referral.reward;

import com.actpro.referral.campaign.Campaign;
import com.actpro.referral.campaign.RewardType;
import com.actpro.referral.company.Company;
import com.actpro.referral.conversion.Conversion;
import com.actpro.referral.reward.dto.RewardResult;
import com.actpro.referral.user.PlatformUser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RewardServiceTest {

    @Mock
    private RewardRepository rewardRepository;

    @Mock
    private CouponCodeGenerator couponCodeGenerator;

    @InjectMocks
    private RewardService rewardService;

    private Company company;
    private Campaign campaign;
    private Conversion conversion;

    @BeforeEach
    void setUp() {
        company = new Company();
        company.setId(1L);

        campaign = new Campaign();
        campaign.setId(2L);
        campaign.setRewardType(RewardType.CREDIT);
        campaign.setReferrerRewardValue(new BigDecimal("10.00"));
        campaign.setRefereeRewardValue(new BigDecimal("5.00"));

        conversion = new Conversion();
        conversion.setId(3L);
        conversion.setCompany(company);
        conversion.setCampaign(campaign);

        PlatformUser referee = new PlatformUser();
        referee.setId(21L);
        conversion.setRefereeUser(referee);
    }

    @Test
    void shouldIssueBothRewardsForLegacyDirectApiConversion() {
        PlatformUser referrer = new PlatformUser();
        referrer.setId(20L);
        conversion.setReferrerUser(referrer);

        when(couponCodeGenerator.generate()).thenReturn("REF-AAAA1111", "REF-BBBB2222");

        RewardResult result = rewardService.issueRewards(conversion);

        assertNotNull(result.getReferrerReward());
        assertNotNull(result.getRefereeReward());
        assertNull(result.getAmbassadorReward());
        verify(rewardRepository, times(2)).save(any(Reward.class));
    }

    @Test
    void shouldSkipReferrerRewardForAmbassadorDrivenConversion() {
        conversion.setReferrerUser(null);
        when(couponCodeGenerator.generate()).thenReturn("REF-CCCC3333");

        RewardResult result = rewardService.issueRewards(conversion);

        assertNull(result.getReferrerReward());
        assertNotNull(result.getRefereeReward());
        verify(rewardRepository, times(1)).save(any(Reward.class));
    }
}
