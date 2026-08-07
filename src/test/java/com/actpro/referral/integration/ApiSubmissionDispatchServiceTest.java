package com.actpro.referral.integration;

import com.actpro.referral.campaign.Campaign;
import com.actpro.referral.company.Company;
import com.actpro.referral.company.CompanyIntegration;
import com.actpro.referral.company.CompanyIntegrationRepository;
import com.actpro.referral.company.CompanyIntegrationStatus;
import com.actpro.referral.integration.dto.CreateUserApiCallResult;
import com.actpro.referral.integration.dto.CreateUserApiRequestPayload;
import com.actpro.referral.referral.Referral;
import com.actpro.referral.referral.ReferralRepository;
import com.actpro.referral.user.PlatformUser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ApiSubmissionDispatchServiceTest {

    @Mock
    private ApiSubmissionRepository apiSubmissionRepository;

    @Mock
    private IntegrationAttemptRepository integrationAttemptRepository;

    @Mock
    private CompanyIntegrationRepository companyIntegrationRepository;

    @Mock
    private ReferralRepository referralRepository;

    @Mock
    private CreateUserApiClient createUserApiClient;

    @InjectMocks
    private ApiSubmissionDispatchService dispatchService;

    private Company company;
    private CompanyIntegration integration;
    private Referral referral;
    private ApiSubmission submission;

    @BeforeEach
    void setUp() {
        company = new Company();
        company.setId(7L);

        integration = new CompanyIntegration();
        integration.setCompany(company);
        integration.setStatus(CompanyIntegrationStatus.ACTIVE);

        PlatformUser customer = new PlatformUser();
        customer.setName("Jane Doe");
        customer.setEmail("jane@example.com");

        Campaign campaign = new Campaign();
        campaign.setCampaignCode("SUMMER2026");

        referral = new Referral();
        referral.setId(42L);
        referral.setCustomerUser(customer);
        referral.setCampaign(campaign);
        referral.setReferralCode("ABC123");

        submission = new ApiSubmission();
        submission.setId(1L);
        submission.setCompany(company);
        submission.setAggregateId(42L);
        submission.setAggregateType("REFERRAL");
        submission.setSourceEventType("referral.lead_registered");
        submission.setExternalRequestId("req_1");
        submission.setStatus(ApiSubmissionStatus.PENDING);
        submission.setAttempts(0);
        submission.setMaxAttempts(5);
        submission.setAvailableAt(LocalDateTime.now());

        // Not every test reaches dispatchOne (claimBatch tests don't), so these are lenient rather
        // than strict - they're the common-case fixture for the dispatchOne tests below.
        org.mockito.Mockito.lenient().when(companyIntegrationRepository.findByCompanyId(7L)).thenReturn(Optional.of(integration));
        org.mockito.Mockito.lenient().when(referralRepository.findById(42L)).thenReturn(Optional.of(referral));
    }

    @Test
    void shouldReturnEmptyBatchWithoutQueryingWhenNothingClaimed() {
        when(apiSubmissionRepository.claimBatch(eq("token-1"), any(LocalDateTime.class), eq(50))).thenReturn(0);

        List<ApiSubmission> batch = dispatchService.claimBatch("token-1", 50);

        assertTrue(batch.isEmpty());
        verify(apiSubmissionRepository, never()).findByLockedBy(any());
    }

    @Test
    void shouldReturnClaimedRowsByLockToken() {
        when(apiSubmissionRepository.claimBatch(eq("token-2"), any(LocalDateTime.class), eq(50))).thenReturn(1);
        when(apiSubmissionRepository.findByLockedBy("token-2")).thenReturn(List.of(submission));

        List<ApiSubmission> batch = dispatchService.claimBatch("token-2", 50);

        assertEquals(1, batch.size());
    }

    @Test
    void shouldMarkSucceededOnHttp2xxAndStoreReferences() {
        when(createUserApiClient.call(eq(integration), any(CreateUserApiRequestPayload.class)))
                .thenReturn(CreateUserApiCallResult.httpResponse(201, "{\"customerId\":\"cust_1\"}", "cust_1", "txn_1"));

        dispatchService.dispatchOne(submission);

        assertEquals(ApiSubmissionStatus.SUCCEEDED, submission.getStatus());
        assertEquals(1, submission.getAttempts());
        assertNotNull(submission.getSubmittedAt());
        assertEquals("cust_1", submission.getCompanyCustomerReference());
        assertEquals("txn_1", submission.getCompanyTransactionReference());
        assertNull(submission.getLastError());

        ArgumentCaptor<IntegrationAttempt> captor = ArgumentCaptor.forClass(IntegrationAttempt.class);
        verify(integrationAttemptRepository).save(captor.capture());
        assertEquals(AttemptOutcome.SUCCESS, captor.getValue().getOutcome());
        assertEquals(1, captor.getValue().getAttemptNumber());
        assertEquals(201, captor.getValue().getHttpStatus());
    }

    @Test
    void shouldScheduleRetryWithFutureAvailableAtOnServerError() {
        when(createUserApiClient.call(eq(integration), any())).thenReturn(CreateUserApiCallResult.httpResponse(503, null, null, null));

        dispatchService.dispatchOne(submission);

        assertEquals(ApiSubmissionStatus.RETRY_SCHEDULED, submission.getStatus());
        assertTrue(submission.getAvailableAt().isAfter(LocalDateTime.now()));
        verify(companyIntegrationRepository, never()).save(any());

        ArgumentCaptor<IntegrationAttempt> captor = ArgumentCaptor.forClass(IntegrationAttempt.class);
        verify(integrationAttemptRepository).save(captor.capture());
        assertEquals(FailureCategory.SERVER_ERROR, captor.getValue().getFailureCategory());
        assertNotNull(captor.getValue().getNextRetryAt());
    }

    @Test
    void shouldPermanentlyFailImmediatelyOnClientError() {
        when(createUserApiClient.call(eq(integration), any())).thenReturn(CreateUserApiCallResult.httpResponse(422, null, null, null));

        dispatchService.dispatchOne(submission);

        assertEquals(ApiSubmissionStatus.PERMANENTLY_FAILED, submission.getStatus());

        ArgumentCaptor<IntegrationAttempt> captor = ArgumentCaptor.forClass(IntegrationAttempt.class);
        verify(integrationAttemptRepository).save(captor.capture());
        assertEquals(FailureCategory.CLIENT_ERROR, captor.getValue().getFailureCategory());
        assertNull(captor.getValue().getNextRetryAt());
    }

    @Test
    void shouldPermanentlyFailAndFlipIntegrationToErrorOnAuthFailure() {
        when(createUserApiClient.call(eq(integration), any())).thenReturn(CreateUserApiCallResult.httpResponse(401, null, null, null));

        dispatchService.dispatchOne(submission);

        assertEquals(ApiSubmissionStatus.PERMANENTLY_FAILED, submission.getStatus());
        assertEquals(CompanyIntegrationStatus.ERROR, integration.getStatus());
        verify(companyIntegrationRepository).save(integration);
    }

    @Test
    void shouldRetryOnConnectionErrorIoFailure() {
        when(createUserApiClient.call(eq(integration), any()))
                .thenReturn(CreateUserApiCallResult.ioFailure(FailureCategory.CONNECTION_ERROR, "connection refused"));

        dispatchService.dispatchOne(submission);

        assertEquals(ApiSubmissionStatus.RETRY_SCHEDULED, submission.getStatus());
        assertEquals("connection refused", submission.getLastError());
    }

    @Test
    void shouldRetryOnTimeoutIoFailure() {
        // Phase 9 hardening: TIMEOUT is in TRANSIENT_CATEGORIES alongside CONNECTION_ERROR/
        // SERVER_ERROR/RATE_LIMITED, but only CONNECTION_ERROR/SERVER_ERROR had their own test
        // above - closing the gap for the other two.
        when(createUserApiClient.call(eq(integration), any()))
                .thenReturn(CreateUserApiCallResult.ioFailure(FailureCategory.TIMEOUT, "read timed out"));

        dispatchService.dispatchOne(submission);

        assertEquals(ApiSubmissionStatus.RETRY_SCHEDULED, submission.getStatus());
        assertEquals("read timed out", submission.getLastError());
        assertTrue(submission.getAvailableAt().isAfter(LocalDateTime.now()));
    }

    @Test
    void shouldRetryOnRateLimitedResponse() {
        when(createUserApiClient.call(eq(integration), any())).thenReturn(CreateUserApiCallResult.httpResponse(429, null, null, null));

        dispatchService.dispatchOne(submission);

        assertEquals(ApiSubmissionStatus.RETRY_SCHEDULED, submission.getStatus());

        ArgumentCaptor<IntegrationAttempt> captor = ArgumentCaptor.forClass(IntegrationAttempt.class);
        verify(integrationAttemptRepository).save(captor.capture());
        assertEquals(FailureCategory.RATE_LIMITED, captor.getValue().getFailureCategory());
    }

    @Test
    void shouldPermanentlyFailWhenTransientFailureExceedsMaxAttempts() {
        submission.setAttempts(4); // 5th attempt, maxAttempts = 5 -> no further retry
        when(createUserApiClient.call(eq(integration), any())).thenReturn(CreateUserApiCallResult.httpResponse(503, null, null, null));

        dispatchService.dispatchOne(submission);

        assertEquals(ApiSubmissionStatus.PERMANENTLY_FAILED, submission.getStatus());
        assertEquals(5, submission.getAttempts());
    }

    @Test
    void shouldRecordSequentialAttemptNumbers() {
        submission.setAttempts(2);
        when(createUserApiClient.call(eq(integration), any())).thenReturn(CreateUserApiCallResult.httpResponse(503, null, null, null));

        dispatchService.dispatchOne(submission);

        ArgumentCaptor<IntegrationAttempt> captor = ArgumentCaptor.forClass(IntegrationAttempt.class);
        verify(integrationAttemptRepository, times(1)).save(captor.capture());
        assertEquals(3, captor.getValue().getAttemptNumber());
        assertEquals(3, submission.getAttempts());
    }

    @Test
    void shouldPermanentlyFailWhenReferralNoLongerExists() {
        when(referralRepository.findById(42L)).thenReturn(Optional.empty());

        dispatchService.dispatchOne(submission);

        assertEquals(ApiSubmissionStatus.PERMANENTLY_FAILED, submission.getStatus());
        verify(createUserApiClient, never()).call(any(), any());
    }
}
