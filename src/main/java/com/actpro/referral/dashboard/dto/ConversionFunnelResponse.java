package com.actpro.referral.dashboard.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ConversionFunnelResponse {

    private Long campaignId;
    private String campaignName;

    private Long referralsCount;
    private Long clicksCount;
    private Long conversionsCount;

    private Double clickThroughRate;
    private Double conversionRate;
    private Double overallConversionRate;
}
