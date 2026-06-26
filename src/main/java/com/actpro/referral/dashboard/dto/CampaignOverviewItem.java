package com.actpro.referral.dashboard.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CampaignOverviewItem {

    private Long campaignId;
    private String campaignName;
    private String status;

    private Long referralCount;
    private Long clickCount;
    private Long conversionCount;

    private Double conversionRate;
}
