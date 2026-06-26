package com.actpro.referral.dashboard.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CampaignStatsResponse {

    private Long campaignId;
    private String campaignName;
    private String status;

    private Long totalReferrals;
    private Long totalClicks;
    private Long totalConversions;

    private Double clickThroughRate;
    private Double conversionRate;

    private Long totalRewardsIssued;
    private BigDecimal totalRewardValue;
    private Long rewardsRedeemed;

    private BigDecimal averageRewardValue;
}
