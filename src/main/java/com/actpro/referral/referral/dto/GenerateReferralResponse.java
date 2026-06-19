package com.actpro.referral.referral.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class GenerateReferralResponse {
    private String referralCode;
    private String referralLink;
}
