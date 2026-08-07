package com.actpro.referral.ambassador;

import com.actpro.referral.ambassador.dto.AmbassadorEarningsHistoryResponse;
import com.actpro.referral.ambassador.dto.AmbassadorEarningsSummaryResponse;
import com.actpro.referral.security.ApiKeyAuthenticationFilter;
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

import java.math.BigDecimal;
import java.util.List;

import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Covers only the new Phase 8 earnings endpoints - no existing AmbassadorPortalController security test to extend. */
@WebMvcTest(AmbassadorPortalController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(AmbassadorPortalEarningsSecurityTest.MethodSecurityTestConfig.class)
class AmbassadorPortalEarningsSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AmbassadorPortalService ambassadorPortalService;

    @MockBean
    private ApiKeyAuthenticationFilter apiKeyAuthenticationFilter;

    @MockBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @Test
    @WithMockUser(roles = "AMBASSADOR")
    void shouldAllowAmbassadorToGetEarningsSummary() throws Exception {
        when(ambassadorPortalService.getEarningsSummary()).thenReturn(
                new AmbassadorEarningsSummaryResponse(BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, 0, "USD"));

        mockMvc.perform(get("/api/ambassador/earnings"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "AMBASSADOR")
    void shouldAllowAmbassadorToListEarningsHistory() throws Exception {
        when(ambassadorPortalService.listEarnings(0, 20)).thenReturn(
                new AmbassadorEarningsHistoryResponse(List.of(), 0, 20, 0, 0));

        mockMvc.perform(get("/api/ambassador/earnings/history"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "COMPANY_ADMIN")
    void shouldBlockCompanyAdminFromAmbassadorEarnings() throws Exception {
        mockMvc.perform(get("/api/ambassador/earnings"))
                .andExpect(status().isForbidden());

        verifyNoInteractions(ambassadorPortalService);
    }

    @TestConfiguration
    @EnableMethodSecurity
    static class MethodSecurityTestConfig {
    }
}
