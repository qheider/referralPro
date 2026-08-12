package com.actpro.referral.integration.webhook;

import com.actpro.referral.common.exception.BadRequestException;
import com.actpro.referral.common.exception.NotFoundException;
import com.actpro.referral.common.exception.UnauthorizedException;
import com.actpro.referral.company.Company;
import com.actpro.referral.company.CompanyIntegration;
import com.actpro.referral.company.CompanyIntegrationRepository;
import com.actpro.referral.integration.CredentialEncryptionService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WebhookIngestServiceTest {

    @Mock
    private CompanyIntegrationRepository companyIntegrationRepository;

    @Mock
    private CredentialEncryptionService credentialEncryptionService;

    @Mock
    private WebhookSignatureVerifier webhookSignatureVerifier;

    @Mock
    private WebhookEventRepository webhookEventRepository;

    private WebhookIngestService webhookIngestService;

    private CompanyIntegration integration;
    private static final String VALID_BODY = "{\"eventId\":\"evt_1\",\"eventType\":\"SERVICE_COMPLETED\",\"status\":\"COMPLETED\"}";

    @BeforeEach
    void setUp() {
        webhookIngestService = new WebhookIngestService(
                companyIntegrationRepository, credentialEncryptionService, webhookSignatureVerifier,
                webhookEventRepository, new ObjectMapper());
        ReflectionTestUtils.setField(webhookIngestService, "toleranceSeconds", 300);
        ReflectionTestUtils.setField(webhookIngestService, "defaultMaxAttempts", 5);

        Company company = new Company();
        company.setId(9L);
        integration = new CompanyIntegration();
        integration.setCompany(company);
        integration.setWebhookPublicId("CODE123");
        integration.setWebhookSigningSecret("encrypted-secret");
    }

    @Test
    void shouldStoreEventOnFirstDelivery() {
        when(companyIntegrationRepository.findByWebhookPublicId("CODE123")).thenReturn(Optional.of(integration));
        when(credentialEncryptionService.decrypt("encrypted-secret")).thenReturn("raw-secret");
        when(webhookSignatureVerifier.isValid(VALID_BODY, "sig", "123", "raw-secret", 300)).thenReturn(true);
        when(webhookEventRepository.insertIgnoreDuplicate(eq(9L), eq("evt_1"), eq("SERVICE_COMPLETED"), eq(VALID_BODY), eq(5), any(LocalDateTime.class)))
                .thenReturn(1);

        webhookIngestService.ingest("CODE123", "sig", "123", VALID_BODY);

        verify(webhookEventRepository).insertIgnoreDuplicate(eq(9L), eq("evt_1"), eq("SERVICE_COMPLETED"), eq(VALID_BODY), eq(5), any(LocalDateTime.class));
    }

    @Test
    void shouldAckDuplicateWithoutThrowing() {
        when(companyIntegrationRepository.findByWebhookPublicId("CODE123")).thenReturn(Optional.of(integration));
        when(credentialEncryptionService.decrypt("encrypted-secret")).thenReturn("raw-secret");
        when(webhookSignatureVerifier.isValid(VALID_BODY, "sig", "123", "raw-secret", 300)).thenReturn(true);
        when(webhookEventRepository.insertIgnoreDuplicate(any(), any(), any(), any(), anyInt(), any())).thenReturn(0);

        assertDoesNotThrow(() -> webhookIngestService.ingest("CODE123", "sig", "123", VALID_BODY));
    }

    @Test
    void shouldRejectUnknownCompanyCode() {
        when(companyIntegrationRepository.findByWebhookPublicId("UNKNOWN")).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> webhookIngestService.ingest("UNKNOWN", "sig", "123", VALID_BODY));
        verify(webhookEventRepository, never()).insertIgnoreDuplicate(any(), any(), any(), any(), anyInt(), any());
    }

    @Test
    void shouldRejectMissingSignatureHeader() {
        when(companyIntegrationRepository.findByWebhookPublicId("CODE123")).thenReturn(Optional.of(integration));

        assertThrows(UnauthorizedException.class, () -> webhookIngestService.ingest("CODE123", null, "123", VALID_BODY));
        verify(webhookEventRepository, never()).insertIgnoreDuplicate(any(), any(), any(), any(), anyInt(), any());
    }

    @Test
    void shouldRejectWhenNoWebhookSecretConfigured() {
        integration.setWebhookSigningSecret(null);
        when(companyIntegrationRepository.findByWebhookPublicId("CODE123")).thenReturn(Optional.of(integration));

        assertThrows(UnauthorizedException.class, () -> webhookIngestService.ingest("CODE123", "sig", "123", VALID_BODY));
        verify(webhookEventRepository, never()).insertIgnoreDuplicate(any(), any(), any(), any(), anyInt(), any());
    }

    @Test
    void shouldRejectInvalidSignature() {
        when(companyIntegrationRepository.findByWebhookPublicId("CODE123")).thenReturn(Optional.of(integration));
        when(credentialEncryptionService.decrypt("encrypted-secret")).thenReturn("raw-secret");
        when(webhookSignatureVerifier.isValid(VALID_BODY, "sig", "123", "raw-secret", 300)).thenReturn(false);

        assertThrows(UnauthorizedException.class, () -> webhookIngestService.ingest("CODE123", "sig", "123", VALID_BODY));
        verify(webhookEventRepository, never()).insertIgnoreDuplicate(any(), any(), any(), any(), anyInt(), any());
    }

    @Test
    void shouldRejectMalformedJson() {
        when(companyIntegrationRepository.findByWebhookPublicId("CODE123")).thenReturn(Optional.of(integration));
        when(credentialEncryptionService.decrypt("encrypted-secret")).thenReturn("raw-secret");
        when(webhookSignatureVerifier.isValid(any(), any(), any(), any(), anyInt())).thenReturn(true);

        assertThrows(BadRequestException.class, () -> webhookIngestService.ingest("CODE123", "sig", "123", "not-json"));
        verify(webhookEventRepository, never()).insertIgnoreDuplicate(any(), any(), any(), any(), anyInt(), any());
    }

    @Test
    void shouldRejectMissingEventId() {
        when(companyIntegrationRepository.findByWebhookPublicId("CODE123")).thenReturn(Optional.of(integration));
        when(credentialEncryptionService.decrypt("encrypted-secret")).thenReturn("raw-secret");
        when(webhookSignatureVerifier.isValid(any(), any(), any(), any(), anyInt())).thenReturn(true);

        assertThrows(BadRequestException.class,
                () -> webhookIngestService.ingest("CODE123", "sig", "123", "{\"eventType\":\"SERVICE_COMPLETED\"}"));
        verify(webhookEventRepository, never()).insertIgnoreDuplicate(any(), any(), any(), any(), anyInt(), any());
    }
}
