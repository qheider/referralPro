package com.actpro.referral.integration;

import com.actpro.referral.common.exception.BadRequestException;
import com.actpro.referral.company.Company;
import com.actpro.referral.company.CompanyIntegration;
import com.actpro.referral.company.CompanyIntegrationRepository;
import com.actpro.referral.company.CompanyIntegrationStatus;
import com.actpro.referral.integration.dto.CompanyIntegrationConfigResponse;
import com.actpro.referral.integration.dto.CreateUserApiCallResult;
import com.actpro.referral.integration.dto.TestConnectionResponse;
import com.actpro.referral.integration.dto.UpdateCompanyIntegrationConfigRequest;
import com.actpro.referral.integration.webhook.ReferralStatusMappingService;
import com.actpro.referral.integration.webhook.WebhookEventRepository;
import com.actpro.referral.integration.webhook.dto.GenerateWebhookSecretResponse;
import com.actpro.referral.revenue.RewardStatusMappingService;
import com.actpro.referral.security.CurrentUserService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CompanyIntegrationServiceTest {

    @Mock
    private CompanyIntegrationRepository companyIntegrationRepository;

    @Mock
    private ApiSubmissionRepository apiSubmissionRepository;

    @Mock
    private IntegrationAttemptRepository integrationAttemptRepository;

    @Mock
    private WebhookEventRepository webhookEventRepository;

    @Mock
    private CreateUserApiClient createUserApiClient;

    @Mock
    private CurrentUserService currentUserService;

    private CompanyIntegrationService companyIntegrationService;

    private CompanyIntegration integration;

    @BeforeEach
    void setUp() {
        // Real (not mocked) collaborators - no external dependencies, safe to use as-is.
        companyIntegrationService = new CompanyIntegrationService(
                companyIntegrationRepository, apiSubmissionRepository, integrationAttemptRepository, webhookEventRepository,
                new CredentialEncryptionService("test-key"), createUserApiClient, new ReferralStatusMappingService(new ObjectMapper()),
                new RewardStatusMappingService(new ObjectMapper()), currentUserService, new ObjectMapper());
        ReflectionTestUtils.setField(companyIntegrationService, "baseUrl", "http://localhost:8080");

        Company company = new Company();
        company.setId(7L);
        integration = new CompanyIntegration();
        integration.setCompany(company);
        integration.setWebhookPublicId("CODE123");
        integration.setStatus(CompanyIntegrationStatus.NOT_CONFIGURED);

        when(currentUserService.getCurrentCompanyId()).thenReturn(7L);
        when(companyIntegrationRepository.findByCompanyId(7L)).thenReturn(Optional.of(integration));
        // Not every test reaches a save() (the reject-* tests throw before then).
        org.mockito.Mockito.lenient().when(companyIntegrationRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
    }

    @Test
    void shouldRejectApiKeyAuthWithoutRequiredCredentialFields() {
        UpdateCompanyIntegrationConfigRequest request = new UpdateCompanyIntegrationConfigRequest(
                "https://company.example.com/create-user", IntegrationAuthType.API_KEY,
                null, null, null, null, null, null, null, null, null);

        assertThrows(BadRequestException.class, () -> companyIntegrationService.updateConfig(request));
    }

    @Test
    void shouldRejectBasicAuthMissingPassword() {
        UpdateCompanyIntegrationConfigRequest request = new UpdateCompanyIntegrationConfigRequest(
                "https://company.example.com/create-user", IntegrationAuthType.BASIC,
                null, null, null, "user", null, null, null, null, null);

        assertThrows(BadRequestException.class, () -> companyIntegrationService.updateConfig(request));
    }

    @Test
    void shouldAcceptBearerTokenAndFlipToPendingVerification() {
        UpdateCompanyIntegrationConfigRequest request = new UpdateCompanyIntegrationConfigRequest(
                "https://company.example.com/create-user", IntegrationAuthType.BEARER_TOKEN,
                null, null, "a-token", null, null, null, null, null, null);

        CompanyIntegrationConfigResponse response = companyIntegrationService.updateConfig(request);

        assertEquals(CompanyIntegrationStatus.PENDING_VERIFICATION, response.status());
        assertTrue(response.hasCredentials());
    }

    @Test
    void shouldRejectInvalidStatusMappingJson() {
        UpdateCompanyIntegrationConfigRequest request = new UpdateCompanyIntegrationConfigRequest(
                "https://company.example.com/create-user", IntegrationAuthType.NONE,
                null, null, null, null, null, null, null, "not-json", null);

        assertThrows(BadRequestException.class, () -> companyIntegrationService.updateConfig(request));
    }

    @Test
    void shouldKeepExistingCredentialsWhenNotResuppliedOnUpdate() {
        integration.setAuthType(IntegrationAuthType.BEARER_TOKEN);
        integration.setEncryptedCredentials(new CredentialEncryptionService("test-key").encrypt("{\"token\":\"old\"}"));
        String existing = integration.getEncryptedCredentials();

        UpdateCompanyIntegrationConfigRequest request = new UpdateCompanyIntegrationConfigRequest(
                "https://company.example.com/create-user", IntegrationAuthType.BEARER_TOKEN,
                null, null, null, null, null, null, null, null, null);

        companyIntegrationService.updateConfig(request);

        assertEquals(existing, integration.getEncryptedCredentials());
    }

    @Test
    void shouldClearCredentialsWhenAuthTypeSwitchedToNone() {
        integration.setAuthType(IntegrationAuthType.BEARER_TOKEN);
        integration.setEncryptedCredentials(new CredentialEncryptionService("test-key").encrypt("{\"token\":\"old\"}"));

        UpdateCompanyIntegrationConfigRequest request = new UpdateCompanyIntegrationConfigRequest(
                "https://company.example.com/create-user", IntegrationAuthType.NONE,
                null, null, null, null, null, null, null, null, null);

        CompanyIntegrationConfigResponse response = companyIntegrationService.updateConfig(request);

        assertFalse(response.hasCredentials());
    }

    @Test
    void shouldMarkActiveOnSuccessfulTestConnection() {
        integration.setApiBaseUrl("https://company.example.com/create-user");
        when(createUserApiClient.testConnection(integration)).thenReturn(CreateUserApiCallResult.httpResponse(200, "ok", null, null));

        TestConnectionResponse response = companyIntegrationService.testConnection();

        assertTrue(response.success());
        assertEquals(CompanyIntegrationStatus.ACTIVE, response.resultingStatus());
    }

    @Test
    void shouldMarkErrorOnAuthFailureDuringTestConnection() {
        integration.setApiBaseUrl("https://company.example.com/create-user");
        when(createUserApiClient.testConnection(integration)).thenReturn(CreateUserApiCallResult.httpResponse(401, null, null, null));

        TestConnectionResponse response = companyIntegrationService.testConnection();

        assertFalse(response.success());
        assertEquals(CompanyIntegrationStatus.ERROR, response.resultingStatus());
    }

    @Test
    void shouldFailWithoutFlippingToErrorOnConnectionFailureDuringTestConnection() {
        integration.setApiBaseUrl("https://company.example.com/create-user");
        integration.setStatus(CompanyIntegrationStatus.PENDING_VERIFICATION);
        when(createUserApiClient.testConnection(integration))
                .thenReturn(CreateUserApiCallResult.ioFailure(FailureCategory.CONNECTION_ERROR, "connection refused"));

        TestConnectionResponse response = companyIntegrationService.testConnection();

        assertFalse(response.success());
        assertEquals(CompanyIntegrationStatus.PENDING_VERIFICATION, response.resultingStatus());
    }

    @Test
    void shouldRejectEnableWhenNotDisabled() {
        integration.setStatus(CompanyIntegrationStatus.ACTIVE);

        assertThrows(BadRequestException.class, () -> companyIntegrationService.enable());
    }

    @Test
    void shouldEnableFromDisabled() {
        integration.setStatus(CompanyIntegrationStatus.DISABLED);

        CompanyIntegrationConfigResponse response = companyIntegrationService.enable();

        assertEquals(CompanyIntegrationStatus.ACTIVE, response.status());
    }

    @Test
    void shouldRejectDisableWhenAlreadyDisabled() {
        integration.setStatus(CompanyIntegrationStatus.DISABLED);

        assertThrows(BadRequestException.class, () -> companyIntegrationService.disable());
    }

    @Test
    void shouldGenerateWebhookSecretAndEncryptBeforeStorage() {
        GenerateWebhookSecretResponse response = companyIntegrationService.generateWebhookSecret();

        assertNotNull(response.webhookSecret());
        // Stored value must not be the raw secret itself (i.e. it went through encryption).
        assertTrue(integration.getWebhookSigningSecret() != null && !integration.getWebhookSigningSecret().equals(response.webhookSecret()));
    }

    @Test
    void shouldRejectStatusMappingValueThatIsNotARealReferralStatus() {
        UpdateCompanyIntegrationConfigRequest request = new UpdateCompanyIntegrationConfigRequest(
                "https://company.example.com/create-user", IntegrationAuthType.NONE,
                null, null, null, null, null, null, null, "{\"SERVICE_DONE\":\"NOT_A_REAL_STATUS\"}", null);

        assertThrows(BadRequestException.class, () -> companyIntegrationService.updateConfig(request));
    }

    @Test
    void shouldRejectStatusMappingValueOfExpired() {
        UpdateCompanyIntegrationConfigRequest request = new UpdateCompanyIntegrationConfigRequest(
                "https://company.example.com/create-user", IntegrationAuthType.NONE,
                null, null, null, null, null, null, null, "{\"SERVICE_CANCELLED\":\"EXPIRED\"}", null);

        assertThrows(BadRequestException.class, () -> companyIntegrationService.updateConfig(request));
    }

    @Test
    void shouldIncludeWebhookUrlAndHasWebhookSigningSecretInConfigResponse() {
        integration.setWebhookSigningSecret("encrypted-secret");

        CompanyIntegrationConfigResponse response = companyIntegrationService.getConfig();

        assertEquals("CODE123", response.webhookPublicId());
        assertEquals("http://localhost:8080/api/v1/integrations/CODE123/webhooks/service-status", response.webhookUrl());
        assertTrue(response.hasWebhookSigningSecret());
    }
}
