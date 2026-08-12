package com.actpro.referral.integration;

import com.actpro.referral.company.CompanyIntegrationStatus;
import com.actpro.referral.integration.dto.CompanyIntegrationConfigResponse;
import com.actpro.referral.integration.dto.TestConnectionResponse;
import com.actpro.referral.integration.webhook.dto.GenerateWebhookSecretResponse;
import com.actpro.referral.security.ApiKeyAuthenticationFilter;
import com.actpro.referral.security.CurrentUserService;
import com.actpro.referral.security.JwtAuthenticationFilter;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(CompanyIntegrationAdminController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(CompanyIntegrationAdminControllerTest.MethodSecurityTestConfig.class)
class CompanyIntegrationAdminControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private CompanyIntegrationService companyIntegrationService;

    @MockBean
    private CurrentUserService currentUserService;

    @MockBean
    private ApiKeyAuthenticationFilter apiKeyAuthenticationFilter;

    @MockBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @Test
    @WithMockUser(roles = "COMPANY_ADMIN")
    void shouldGetConfigWithoutExposingCredentialValues() throws Exception {
        CompanyIntegrationConfigResponse response = new CompanyIntegrationConfigResponse(
                1L, CompanyIntegrationStatus.ACTIVE, "https://company.example.com/create-user",
                IntegrationAuthType.BEARER_TOKEN, true, 10000, 5, null, null,
                LocalDateTime.now(), "SUCCESS", "Reached the configured endpoint (HTTP 200)",
                "CODE123", "http://localhost:8080/api/v1/integrations/CODE123/webhooks/service-status", true,
                LocalDateTime.now(), LocalDateTime.now());
        when(companyIntegrationService.getConfig()).thenReturn(response);

        mockMvc.perform(get("/api/admin/company-integration"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("ACTIVE"))
                .andExpect(jsonPath("$.data.hasCredentials").value(true))
                .andExpect(jsonPath("$.data.hasWebhookSigningSecret").value(true))
                .andExpect(jsonPath("$.data.webhookPublicId").value("CODE123"))
                .andExpect(jsonPath("$.data.apiKeyValue").doesNotExist())
                .andExpect(jsonPath("$.data.bearerToken").doesNotExist())
                .andExpect(jsonPath("$.data.basicPassword").doesNotExist())
                .andExpect(jsonPath("$.data.webhookSigningSecret").doesNotExist());
    }

    @Test
    @WithMockUser(roles = "COMPANY_ADMIN")
    void shouldUpdateConfigForCompanyAdmin() throws Exception {
        CompanyIntegrationConfigResponse response = new CompanyIntegrationConfigResponse(
                1L, CompanyIntegrationStatus.PENDING_VERIFICATION, "https://company.example.com/create-user",
                IntegrationAuthType.NONE, false, 10000, 5, null, null, null, null, null,
                "CODE123", "http://localhost:8080/api/v1/integrations/CODE123/webhooks/service-status", false,
                LocalDateTime.now(), LocalDateTime.now());
        when(companyIntegrationService.updateConfig(org.mockito.ArgumentMatchers.any())).thenReturn(response);

        mockMvc.perform(put("/api/admin/company-integration")
                        .contentType("application/json")
                        .content("{\"apiBaseUrl\":\"https://company.example.com/create-user\",\"authType\":\"NONE\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("PENDING_VERIFICATION"));
    }

    @Test
    @WithMockUser(roles = "COMPANY_ADMIN")
    void shouldTestConnectionForCompanyAdmin() throws Exception {
        when(companyIntegrationService.testConnection())
                .thenReturn(new TestConnectionResponse(true, 200, "Reached the configured endpoint (HTTP 200)", LocalDateTime.now(), CompanyIntegrationStatus.ACTIVE));

        mockMvc.perform(post("/api/admin/company-integration/test-connection"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.success").value(true));
    }

    @Test
    @WithMockUser(roles = "COMPANY_ADMIN")
    void shouldListSubmissionsForCompanyAdmin() throws Exception {
        when(companyIntegrationService.listSubmissions(null, 50)).thenReturn(List.of());

        mockMvc.perform(get("/api/admin/company-integration/submissions"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "COMPANY_ADMIN")
    void shouldGenerateWebhookSecretForCompanyAdmin() throws Exception {
        when(companyIntegrationService.generateWebhookSecret())
                .thenReturn(new GenerateWebhookSecretResponse("raw-webhook-secret", LocalDateTime.now()));

        mockMvc.perform(post("/api/admin/company-integration/webhook-secret"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.webhookSecret").value("raw-webhook-secret"));
    }

    @Test
    @WithMockUser(roles = "COMPANY_ADMIN")
    void shouldListWebhookEventsForCompanyAdmin() throws Exception {
        when(companyIntegrationService.listWebhookEvents(null, 50)).thenReturn(List.of());

        mockMvc.perform(get("/api/admin/company-integration/webhook-events"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "AMBASSADOR")
    void shouldBlockNonCompanyAdminRolesFromGeneratingWebhookSecret() throws Exception {
        mockMvc.perform(post("/api/admin/company-integration/webhook-secret"))
                .andExpect(status().isForbidden());

        verifyNoInteractions(companyIntegrationService);
    }

    @Test
    @WithMockUser(roles = "AMBASSADOR")
    void shouldBlockNonCompanyAdminRolesFromListingWebhookEvents() throws Exception {
        mockMvc.perform(get("/api/admin/company-integration/webhook-events"))
                .andExpect(status().isForbidden());

        verifyNoInteractions(companyIntegrationService);
    }

    @Test
    @WithMockUser(roles = "AMBASSADOR")
    void shouldBlockNonCompanyAdminRolesFromGettingConfig() throws Exception {
        mockMvc.perform(get("/api/admin/company-integration"))
                .andExpect(status().isForbidden());

        verifyNoInteractions(companyIntegrationService);
    }

    @Test
    @WithMockUser(roles = "AMBASSADOR")
    void shouldBlockNonCompanyAdminRolesFromUpdatingConfig() throws Exception {
        mockMvc.perform(put("/api/admin/company-integration")
                        .contentType("application/json")
                        .content("{}"))
                .andExpect(status().isForbidden());

        verifyNoInteractions(companyIntegrationService);
    }

    @Test
    @WithMockUser(roles = "AMBASSADOR")
    void shouldBlockNonCompanyAdminRolesFromTestingConnection() throws Exception {
        mockMvc.perform(post("/api/admin/company-integration/test-connection"))
                .andExpect(status().isForbidden());

        verifyNoInteractions(companyIntegrationService);
    }

    @Test
    @WithMockUser(roles = "AMBASSADOR")
    void shouldBlockNonCompanyAdminRolesFromListingSubmissions() throws Exception {
        mockMvc.perform(get("/api/admin/company-integration/submissions"))
                .andExpect(status().isForbidden());

        verifyNoInteractions(companyIntegrationService);
    }

    @TestConfiguration
    @EnableMethodSecurity
    static class MethodSecurityTestConfig {
    }
}
