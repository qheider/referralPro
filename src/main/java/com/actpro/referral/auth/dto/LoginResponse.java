package com.actpro.referral.auth.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class LoginResponse {

    private String token;
    private String tokenType = "Bearer";
    private Long userId;
    private String username;
    private Long companyId;
    private String companyName;
    private String role;

    public LoginResponse(String token, Long userId, String username, Long companyId, String companyName, String role) {
        this.token = token;
        this.userId = userId;
        this.username = username;
        this.companyId = companyId;
        this.companyName = companyName;
        this.role = role;
    }
}
