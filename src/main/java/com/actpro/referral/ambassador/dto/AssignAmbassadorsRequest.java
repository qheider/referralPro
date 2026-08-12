package com.actpro.referral.ambassador.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record AssignAmbassadorsRequest(
        @NotEmpty(message = "At least one ambassador is required")
        List<@NotNull(message = "Ambassador ID is required") Long> ambassadorIds
) {
}
