package com.actpro.referral.reward;

import com.actpro.referral.campaign.Campaign;
import com.actpro.referral.conversion.Conversion;
import com.actpro.referral.reward.dto.RewardResult;
import com.actpro.referral.user.PlatformUser;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Slf4j
@Service
@RequiredArgsConstructor
public class RewardService {

    private final RewardRepository rewardRepository;
    private final CouponCodeGenerator couponCodeGenerator;

    @Transactional
    public RewardResult issueRewards(Conversion conversion) {
        Campaign campaign = conversion.getCampaign();
        log.info("Issuing rewards for conversion ID: {}", conversion.getId());

        // Ambassador-driven conversions have no PlatformUser referrer (see Conversion.referrerUser's
        // Javadoc) - their reward is issued separately via RevenueEventService/AmbassadorReward
        // instead, so there's nothing to create here for that side.
        Reward referrerReward = conversion.getReferrerUser() != null
                ? createReward(conversion, conversion.getReferrerUser(), campaign.getReferrerRewardValue())
                : null;
        if (referrerReward != null) {
            rewardRepository.save(referrerReward);
        }

        // Create referee reward - always a real PlatformUser regardless of flow.
        Reward refereeReward = createReward(
                conversion,
                conversion.getRefereeUser(),
                campaign.getRefereeRewardValue()
        );
        rewardRepository.save(refereeReward);

        log.info("Rewards issued successfully: referrer={}, referee={}",
                referrerReward != null ? referrerReward.getCouponCode() : "n/a (ambassador-driven)",
                refereeReward.getCouponCode());

        return new RewardResult(referrerReward, refereeReward, null);
    }

    private Reward createReward(Conversion conversion, PlatformUser user, BigDecimal value) {
        Reward reward = new Reward();
        reward.setCompany(conversion.getCompany());
        reward.setCampaign(conversion.getCampaign());
        reward.setConversion(conversion);
        reward.setUser(user);
        reward.setRewardType(conversion.getCampaign().getRewardType());
        reward.setRewardValue(value);
        reward.setCouponCode(couponCodeGenerator.generate());
        reward.setStatus(RewardStatus.ISSUED);
        return reward;
    }
}
