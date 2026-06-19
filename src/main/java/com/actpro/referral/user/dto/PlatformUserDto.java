package com.actpro.referral.user.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PlatformUserDto {
    private Long id;
    private String externalUserId;
    private String email;
    private String name;
}
