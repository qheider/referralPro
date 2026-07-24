package com.actpro.referral.company.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RegisterCompanyResponse {
    private Long companyId;
    private String name;
    private String apiKey;
    private String adminEmail;
}
