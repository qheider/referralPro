package com.actpro.referral.reward;

import com.actpro.referral.security.ApiKeyAuthenticationFilter;
import com.actpro.referral.security.CompanyContext;
import com.actpro.referral.security.JwtAuthenticationFilter;
import com.actpro.referral.company.Company;
import org.junit.jupiter.api.AfterEach;
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

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(RewardController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(RewardControllerSecurityTest.MethodSecurityTestConfig.class)
class RewardControllerSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private RewardRepository rewardRepository;

    @MockBean
    private ApiKeyAuthenticationFilter apiKeyAuthenticationFilter;

    @MockBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @AfterEach
    void clearContext() {
        CompanyContext.clear();
    }

    @Test
    @WithMockUser(roles = "COMPANY")
    void shouldAllowApiKeyAuthenticatedCompanyCallers() throws Exception {
        Company company = new Company();
        company.setId(5L);
        CompanyContext.setCurrentCompany(company);
        when(rewardRepository.findByCompanyIdAndUserExternalUserId(any(), any())).thenReturn(List.of());

        mockMvc.perform(get("/api/rewards/users/ext-user-1"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "AMBASSADOR")
    void shouldBlockAmbassadorJwtCallersFromIntegrationEndpoint() throws Exception {
        mockMvc.perform(get("/api/rewards/users/ext-user-1"))
                .andExpect(status().isForbidden());

        verifyNoInteractions(rewardRepository);
    }

    @Test
    @WithMockUser(roles = "COMPANY_ADMIN")
    void shouldBlockCompanyAdminJwtCallersFromIntegrationEndpoint() throws Exception {
        mockMvc.perform(get("/api/rewards/users/ext-user-1"))
                .andExpect(status().isForbidden());

        verifyNoInteractions(rewardRepository);
    }

    @TestConfiguration
    @EnableMethodSecurity
    static class MethodSecurityTestConfig {
    }
}
