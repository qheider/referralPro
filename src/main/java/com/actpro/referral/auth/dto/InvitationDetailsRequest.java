package com.actpro.referral.auth.dto;

import jakarta.validation.constraints.NotBlank;

public record InvitationDetailsRequest(@NotBlank String token) {
}
