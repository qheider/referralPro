package com.actpro.referral.conversion.dto;

import com.actpro.referral.conversion.ConversionStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ConversionResponse {
    private Long conversionId;
    private ConversionStatus status;
    private RewardInfo referrerReward;
    private RewardInfo refereeReward;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RewardInfo {
        private String couponCode;
        private String rewardValue;
    }
}
