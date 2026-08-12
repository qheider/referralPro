package com.actpro.referral.integration.webhook;

import com.actpro.referral.company.Company;
import com.actpro.referral.company.CompanyIntegration;
import com.actpro.referral.company.CompanyIntegrationRepository;
import com.actpro.referral.integration.ApiSubmission;
import com.actpro.referral.integration.ApiSubmissionRepository;
import com.actpro.referral.integration.webhook.dto.ReferralStatusChangedEventPayload;
import com.actpro.referral.outbox.OutboxEventPublisher;
import com.actpro.referral.referral.Referral;
import com.actpro.referral.referral.ReferralRepository;
import com.actpro.referral.referral.ReferralStatus;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WebhookProcessingServiceTest {

    @Mock
    private WebhookEventRepository webhookEventRepository;

    @Mock
    private CompanyIntegrationRepository companyIntegrationRepository;

    @Mock
    private ApiSubmissionRepository apiSubmissionRepository;

    @Mock
    private ReferralRepository referralRepository;

    @Mock
    private ReferralStatusMappingService referralStatusMappingService;

    @Mock
    private OutboxEventPublisher outboxEventPublisher;

    private WebhookProcessingService service;

    private Company company;
    private CompanyIntegration integration;
    private Referral referral;
    private ApiSubmission submission;

    @BeforeEach
    void setUp() {
        service = new WebhookProcessingService(
                webhookEventRepository, companyIntegrationRepository, apiSubmissionRepository,
                referralRepository, referralStatusMappingService, outboxEventPublisher, new ObjectMapper());

        company = new Company();
        company.setId(9L);

        integration = new CompanyIntegration();
        integration.setCompany(company);
        integration.setStatusMappingJson("{\"SERVICE_COMPLETED\":\"COMPLETED\"}");

        referral = new Referral();
        referral.setId(42L);
        referral.setStatus(ReferralStatus.RENTAL_STARTED);

        submission = new ApiSubmission();
        submission.setCompany(company);
        submission.setAggregateId(42L);
        submission.setCompanyTransactionReference("RENTAL-19482");
        submission.setCompanyCustomerReference("CUS-48392");
    }

    private WebhookEvent event(String rawPayload) {
        WebhookEvent event = new WebhookEvent();
        event.setId(1L);
        event.setCompany(company);
        event.setEventId("evt_1");
        event.setEventType("SERVICE_COMPLETED");
        event.setRawPayload(rawPayload);
        event.setStatus(WebhookEventStatus.PROCESSING);
        event.setAttempts(0);
        event.setMaxAttempts(3);
        event.setAvailableAt(LocalDateTime.now());
        return event;
    }

    @Test
    void shouldReturnEmptyBatchWithoutQueryingWhenNothingClaimed() {
        when(webhookEventRepository.claimBatch(eq("token-1"), any(LocalDateTime.class), eq(50))).thenReturn(0);

        List<WebhookEvent> batch = service.claimBatch("token-1", 50);

        assertTrue(batch.isEmpty());
        verify(webhookEventRepository, never()).findByLockedBy(any());
    }

    @Test
    void shouldMatchByTransactionReferenceFirst() {
        WebhookEvent event = event("{\"eventId\":\"evt_1\",\"eventType\":\"SERVICE_COMPLETED\",\"status\":\"SERVICE_COMPLETED\","
                + "\"serviceReference\":\"RENTAL-19482\",\"companyUserReference\":\"CUS-48392\"}");
        when(apiSubmissionRepository.findByCompanyIdAndCompanyTransactionReference(9L, "RENTAL-19482")).thenReturn(Optional.of(submission));
        when(referralRepository.findById(42L)).thenReturn(Optional.of(referral));
        when(companyIntegrationRepository.findByCompanyId(9L)).thenReturn(Optional.of(integration));
        when(referralStatusMappingService.mapStatus(integration.getStatusMappingJson(), "SERVICE_COMPLETED"))
                .thenReturn(Optional.of(ReferralStatus.COMPLETED));
        when(referralStatusMappingService.isTransitionAllowed(ReferralStatus.RENTAL_STARTED, ReferralStatus.COMPLETED)).thenReturn(true);

        service.processOne(event);

        verify(apiSubmissionRepository, never()).findByCompanyIdAndCompanyCustomerReference(any(), any());
        assertEquals(WebhookEventStatus.PROCESSED, event.getStatus());
        assertEquals(ReferralStatus.COMPLETED, referral.getStatus());
        verify(referralRepository).save(referral);

        ArgumentCaptor<ReferralStatusChangedEventPayload> payloadCaptor = ArgumentCaptor.forClass(ReferralStatusChangedEventPayload.class);
        verify(outboxEventPublisher).publish(eq(company), eq("REFERRAL"), eq(42L), eq("referral.status_changed"), payloadCaptor.capture());
        assertEquals(42L, payloadCaptor.getValue().referralId());
    }

    @Test
    void shouldFallBackToCustomerReferenceWhenTransactionReferenceUnmatched() {
        WebhookEvent event = event("{\"eventId\":\"evt_1\",\"status\":\"SERVICE_COMPLETED\","
                + "\"serviceReference\":\"UNKNOWN-REF\",\"companyUserReference\":\"CUS-48392\"}");
        when(apiSubmissionRepository.findByCompanyIdAndCompanyTransactionReference(9L, "UNKNOWN-REF")).thenReturn(Optional.empty());
        when(apiSubmissionRepository.findByCompanyIdAndCompanyCustomerReference(9L, "CUS-48392")).thenReturn(Optional.of(submission));
        when(referralRepository.findById(42L)).thenReturn(Optional.of(referral));
        when(companyIntegrationRepository.findByCompanyId(9L)).thenReturn(Optional.of(integration));
        when(referralStatusMappingService.mapStatus(any(), any())).thenReturn(Optional.of(ReferralStatus.COMPLETED));
        when(referralStatusMappingService.isTransitionAllowed(any(), any())).thenReturn(true);

        service.processOne(event);

        assertEquals(WebhookEventStatus.PROCESSED, event.getStatus());
    }

    @Test
    void shouldGoToManualReviewWhenNoReferenceMatches() {
        WebhookEvent event = event("{\"eventId\":\"evt_1\",\"status\":\"SERVICE_COMPLETED\","
                + "\"serviceReference\":\"NOPE\",\"companyUserReference\":\"NOPE_TOO\"}");
        when(apiSubmissionRepository.findByCompanyIdAndCompanyTransactionReference(9L, "NOPE")).thenReturn(Optional.empty());
        when(apiSubmissionRepository.findByCompanyIdAndCompanyCustomerReference(9L, "NOPE_TOO")).thenReturn(Optional.empty());

        service.processOne(event);

        assertEquals(WebhookEventStatus.MANUAL_REVIEW, event.getStatus());
        assertTrue(event.getFailureReason().contains("No matching referral"));
        verify(referralRepository, never()).save(any());
        verify(outboxEventPublisher, never()).publish(any(), any(), any(), any(), any());
    }

    @Test
    void shouldGoToManualReviewWhenStatusUnmapped() {
        WebhookEvent event = event("{\"eventId\":\"evt_1\",\"status\":\"WEIRD_STATUS\","
                + "\"serviceReference\":\"RENTAL-19482\"}");
        when(apiSubmissionRepository.findByCompanyIdAndCompanyTransactionReference(9L, "RENTAL-19482")).thenReturn(Optional.of(submission));
        when(referralRepository.findById(42L)).thenReturn(Optional.of(referral));
        when(companyIntegrationRepository.findByCompanyId(9L)).thenReturn(Optional.of(integration));
        when(referralStatusMappingService.mapStatus(integration.getStatusMappingJson(), "WEIRD_STATUS")).thenReturn(Optional.empty());

        service.processOne(event);

        assertEquals(WebhookEventStatus.MANUAL_REVIEW, event.getStatus());
        assertTrue(event.getFailureReason().contains("No status mapping"));
    }

    @Test
    void shouldMarkIgnoredForBackwardTransitionWithoutErroring() {
        WebhookEvent event = event("{\"eventId\":\"evt_1\",\"status\":\"SERVICE_COMPLETED\","
                + "\"serviceReference\":\"RENTAL-19482\"}");
        when(apiSubmissionRepository.findByCompanyIdAndCompanyTransactionReference(9L, "RENTAL-19482")).thenReturn(Optional.of(submission));
        when(referralRepository.findById(42L)).thenReturn(Optional.of(referral));
        when(companyIntegrationRepository.findByCompanyId(9L)).thenReturn(Optional.of(integration));
        when(referralStatusMappingService.mapStatus(any(), any())).thenReturn(Optional.of(ReferralStatus.BOOKING_STARTED));
        when(referralStatusMappingService.isTransitionAllowed(ReferralStatus.RENTAL_STARTED, ReferralStatus.BOOKING_STARTED)).thenReturn(false);

        service.processOne(event);

        assertEquals(WebhookEventStatus.IGNORED, event.getStatus());
        assertEquals(ReferralStatus.RENTAL_STARTED, referral.getStatus());
        verify(referralRepository, never()).save(any());
        verify(outboxEventPublisher, never()).publish(any(), any(), any(), any(), any());
    }

    @Test
    void shouldScheduleRetryOnTransientException() {
        WebhookEvent event = event("not-json");
        event.setAttempts(0);

        service.processOne(event);

        assertEquals(WebhookEventStatus.RETRY_SCHEDULED, event.getStatus());
        assertEquals(1, event.getAttempts());
        assertNull(event.getLockedBy());
        assertTrue(event.getAvailableAt().isAfter(LocalDateTime.now()));
    }

    @Test
    void shouldGoToManualReviewAfterExhaustingRetries() {
        WebhookEvent event = event("not-json");
        event.setAttempts(2); // this failure is the 3rd, hitting maxAttempts=3

        service.processOne(event);

        assertEquals(WebhookEventStatus.MANUAL_REVIEW, event.getStatus());
        assertEquals(3, event.getAttempts());
    }

    @Test
    void shouldClearLockedByAndSaveRegardlessOfOutcome() {
        WebhookEvent event = event("not-json");
        event.setLockedBy("some-token");

        service.processOne(event);

        ArgumentCaptor<WebhookEvent> captor = ArgumentCaptor.forClass(WebhookEvent.class);
        verify(webhookEventRepository).save(captor.capture());
        assertNull(captor.getValue().getLockedBy());
    }
}
