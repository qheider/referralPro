package com.actpro.referral.dashboard.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RewardTypeBreakdown {

    private String rewardType;
    private Long count;
    private BigDecimal totalValue;
}
