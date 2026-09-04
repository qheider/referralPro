package com.actpro.referral.ai.dto;

import java.util.List;

public record AskDashboardResponse(
        String answer,
        List<String> toolsUsed,
        List<ReferencedCampaign> campaigns
) {
}
