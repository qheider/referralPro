package com.actpro.referral.dashboard.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CampaignsOverviewResponse {

    private Long companyId;
    private String companyName;
    private List<CampaignOverviewItem> campaigns;
}
