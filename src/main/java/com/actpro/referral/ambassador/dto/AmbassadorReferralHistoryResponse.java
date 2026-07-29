package com.actpro.referral.ambassador.dto;

import java.util.List;

public record AmbassadorReferralHistoryResponse(
        List<AmbassadorReferralResponse> content,
        int page,
        int size,
        long totalElements,
        int totalPages
) {
}
