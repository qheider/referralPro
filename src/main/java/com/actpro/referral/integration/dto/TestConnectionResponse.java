package com.actpro.referral.integration.dto;

import com.actpro.referral.company.CompanyIntegrationStatus;

import java.time.LocalDateTime;

public record TestConnectionResponse(
        boolean success,
        Integer httpStatus,
        String message,
        LocalDateTime testedAt,
        CompanyIntegrationStatus resultingStatus
) {
}
