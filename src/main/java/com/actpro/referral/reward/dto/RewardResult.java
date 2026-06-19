package com.actpro.referral.reward.dto;

import com.actpro.referral.reward.Reward;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RewardResult {
    private Reward referrerReward;
    private Reward refereeReward;
}
