package com.actpro.referral.reward.dto;

import com.actpro.referral.campaign.RewardType;
import com.actpro.referral.reward.RewardStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RewardResponse {
    private Long rewardId;
    private String couponCode;
    private RewardType rewardType;
    private BigDecimal rewardValue;
    private RewardStatus status;
}
