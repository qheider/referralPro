package com.actpro.referral.integration.dto;

import com.actpro.referral.integration.FailureCategory;

/**
 * Internal return type of {@link com.actpro.referral.integration.CreateUserApiClient} calls.
 * {@code ioSuccess = true} means an HTTP response was received at all (any status code) -
 * categorizing that status into a {@link FailureCategory} is {@code ApiSubmissionDispatchService}'s
 * job, not the client's. {@code ioSuccess = false} means the call never reached an HTTP response
 * (timeout/connection failure) - {@code ioFailureCategory} is pre-set to TIMEOUT or
 * CONNECTION_ERROR in that case.
 */
public record CreateUserApiCallResult(
        boolean ioSuccess,
        Integer httpStatus,
        String responseBody,
        String companyCustomerReference,
        String companyTransactionReference,
        FailureCategory ioFailureCategory,
        String sanitizedErrorMessage
) {

    public static CreateUserApiCallResult ioFailure(FailureCategory category, String sanitizedErrorMessage) {
        return new CreateUserApiCallResult(false, null, null, null, null, category, sanitizedErrorMessage);
    }

    public static CreateUserApiCallResult httpResponse(
            int httpStatus, String responseBody, String companyCustomerReference, String companyTransactionReference) {
        return new CreateUserApiCallResult(true, httpStatus, responseBody, companyCustomerReference, companyTransactionReference, null, null);
    }
}
