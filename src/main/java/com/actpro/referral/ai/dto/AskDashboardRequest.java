package com.actpro.referral.ai.dto;

import jakarta.validation.constraints.NotBlank;

public record AskDashboardRequest(
        @NotBlank(message = "question is required") String question,
        Long campaignId
) {
}
