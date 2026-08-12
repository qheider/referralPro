package com.actpro.referral.ambassador.dto;

import java.util.List;

public record AmbassadorApplicationPageResponse(
        List<AmbassadorApplicationSummaryResponse> content,
        int page,
        int size,
        long totalElements,
        int totalPages,
        boolean first,
        boolean last
) {
}
