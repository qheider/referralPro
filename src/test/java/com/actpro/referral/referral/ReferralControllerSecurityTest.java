package com.actpro.referral.referral;

import com.actpro.referral.referral.dto.GenerateReferralRequest;
import com.actpro.referral.referral.dto.GenerateReferralResponse;
import com.actpro.referral.security.ApiKeyAuthenticationFilter;
import com.actpro.referral.security.JwtAuthenticationFilter;
import com.fasterxml.jackson.databind.ObjectMapper;
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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ReferralController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(ReferralControllerSecurityTest.MethodSecurityTestConfig.class)
class ReferralControllerSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private ReferralService referralService;

    @MockBean
    private ApiKeyAuthenticationFilter apiKeyAuthenticationFilter;

    @MockBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    private static final GenerateReferralRequest REQUEST =
            new GenerateReferralRequest(1L, "ext-user-1", "user@example.com", "User");

    @Test
    @WithMockUser(roles = "COMPANY")
    void shouldAllowApiKeyAuthenticatedCompanyCallers() throws Exception {
        when(referralService.generateReferral(any())).thenReturn(new GenerateReferralResponse("CODE123", "https://example.com/r/CODE123"));

        mockMvc.perform(post("/api/referrals/generate")
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(REQUEST)))
                .andExpect(status().isCreated());
    }

    @Test
    @WithMockUser(roles = "AMBASSADOR")
    void shouldBlockAmbassadorJwtCallersFromIntegrationEndpoint() throws Exception {
        mockMvc.perform(post("/api/referrals/generate")
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(REQUEST)))
                .andExpect(status().isForbidden());

        verifyNoInteractions(referralService);
    }

    @Test
    @WithMockUser(roles = "COMPANY_ADMIN")
    void shouldBlockCompanyAdminJwtCallersFromIntegrationEndpoint() throws Exception {
        mockMvc.perform(post("/api/referrals/generate")
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(REQUEST)))
                .andExpect(status().isForbidden());

        verifyNoInteractions(referralService);
    }

    @TestConfiguration
    @EnableMethodSecurity
    static class MethodSecurityTestConfig {
    }
}
