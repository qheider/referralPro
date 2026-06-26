package com.actpro.referral.dashboard.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TopReferrersResponse {

    private Long campaignId;
    private String campaignName;
    private List<TopReferrerItem> topReferrers;
}
