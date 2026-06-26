package com.actpro.referral.dashboard.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TopReferrerItem {

    private String externalUserId;
    private String userName;
    private String userEmail;

    private Long referralCount;
    private Long clickCount;
    private Long conversionCount;

    private BigDecimal totalRewardsEarned;
}
