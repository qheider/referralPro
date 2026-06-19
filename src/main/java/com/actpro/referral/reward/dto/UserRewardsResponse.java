package com.actpro.referral.reward.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserRewardsResponse {
    private String externalUserId;
    private List<RewardResponse> rewards;
}
