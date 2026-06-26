package com.actpro.referral.dashboard.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RewardSummaryResponse {

    private Long campaignId;
    private String campaignName;

    private Long totalIssued;
    private Long totalRedeemed;
    private Double redemptionRate;

    private BigDecimal totalValueIssued;
    private BigDecimal totalValueRedeemed;

    private List<RewardTypeBreakdown> breakdownByType;
}
