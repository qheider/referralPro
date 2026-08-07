package com.actpro.referral.ambassador.dto;

import java.util.List;

public record AmbassadorEarningsHistoryResponse(
        List<AmbassadorEarningResponse> rewards,
        int page,
        int size,
        long totalElements,
        int totalPages
) {
}
