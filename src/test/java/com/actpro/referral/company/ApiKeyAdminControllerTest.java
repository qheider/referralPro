package com.actpro.referral.company;

import com.actpro.referral.company.dto.ApiKeySummaryResponse;
import com.actpro.referral.company.dto.IssuedApiKeyResponse;
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

import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ApiKeyAdminController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(ApiKeyAdminControllerTest.MethodSecurityTestConfig.class)
class ApiKeyAdminControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private CompanyApiKeyService companyApiKeyService;

    @MockBean
    private CurrentUserService currentUserService;

    @MockBean
    private ApiKeyAuthenticationFilter apiKeyAuthenticationFilter;

    @MockBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @Test
    @WithMockUser(roles = "COMPANY_ADMIN")
    void shouldListKeysForCompanyAdminWithoutExposingSecretHash() throws Exception {
        ApiKeySummaryResponse summary = new ApiKeySummaryResponse(
                1L, "key_abc123", "ab12", CompanyApiKeyStatus.ACTIVE,
                LocalDateTime.now(), null, null, null
        );
        when(companyApiKeyService.listKeys()).thenReturn(List.of(summary));

        mockMvc.perform(get("/api/admin/api-keys"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].keyId").value("key_abc123"))
                .andExpect(jsonPath("$.data[0].secretPreview").value("ab12"))
                .andExpect(jsonPath("$.data[0].secretHash").doesNotExist());
    }

    @Test
    @WithMockUser(roles = "COMPANY_ADMIN")
    void shouldRotateKeyForCompanyAdmin() throws Exception {
        when(companyApiKeyService.rotateKey())
                .thenReturn(new IssuedApiKeyResponse(2L, "key_def456", "cmp_live_newsecret", LocalDateTime.now()));

        mockMvc.perform(post("/api/admin/api-keys/rotate"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.apiKey").value("cmp_live_newsecret"));
    }

    @Test
    @WithMockUser(roles = "COMPANY_ADMIN")
    void shouldRevokeKeyForCompanyAdmin() throws Exception {
        doNothing().when(companyApiKeyService).revokeKey(30L);

        mockMvc.perform(delete("/api/admin/api-keys/30"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "AMBASSADOR")
    void shouldBlockNonCompanyAdminRolesFromListingKeys() throws Exception {
        mockMvc.perform(get("/api/admin/api-keys"))
                .andExpect(status().isForbidden());

        verifyNoInteractions(companyApiKeyService);
    }

    @Test
    @WithMockUser(roles = "AMBASSADOR")
    void shouldBlockNonCompanyAdminRolesFromRotatingKeys() throws Exception {
        mockMvc.perform(post("/api/admin/api-keys/rotate"))
                .andExpect(status().isForbidden());

        verifyNoInteractions(companyApiKeyService);
    }

    @Test
    @WithMockUser(roles = "AMBASSADOR")
    void shouldBlockNonCompanyAdminRolesFromRevokingKeys() throws Exception {
        mockMvc.perform(delete("/api/admin/api-keys/30"))
                .andExpect(status().isForbidden());

        verifyNoInteractions(companyApiKeyService);
    }

    @TestConfiguration
    @EnableMethodSecurity
    static class MethodSecurityTestConfig {
    }
}
