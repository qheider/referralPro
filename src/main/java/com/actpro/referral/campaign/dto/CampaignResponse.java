package com.actpro.referral.campaign.dto;

import com.actpro.referral.campaign.CampaignStatus;
import com.actpro.referral.campaign.RewardType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CampaignResponse {
    private Long campaignId;
    private String name;
    private String description;
    private String landingPageUrl;
    private LocalDateTime startDate;
    private LocalDateTime endDate;
    private RewardType rewardType;
    private BigDecimal referrerRewardValue;
    private BigDecimal refereeRewardValue;
    private String conversionEventName;
    private CampaignStatus status;
    private LocalDateTime createdAt;
}
