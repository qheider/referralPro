package com.actpro.referral.revenue.dto;

import jakarta.validation.constraints.NotBlank;

public record RejectRewardRequest(
        @NotBlank(message = "Rejection reason is required") String reason
) {
}
