package com.actpro.referral.revenue;

import com.actpro.referral.revenue.dto.CampaignRevenueReportResponse;
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

import java.util.List;
import java.util.Map;

import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(RevenueAdminController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(RevenueAdminControllerSecurityTest.MethodSecurityTestConfig.class)
class RevenueAdminControllerSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private RevenueAdminService revenueAdminService;

    @MockBean
    private ApiKeyAuthenticationFilter apiKeyAuthenticationFilter;

    @MockBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @Test
    @WithMockUser(roles = "COMPANY_ADMIN")
    void shouldAllowCompanyAdminToListRewards() throws Exception {
        when(revenueAdminService.listRewards(null, null, 50)).thenReturn(List.of());

        mockMvc.perform(get("/api/admin/revenue/rewards"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "AMBASSADOR")
    void shouldBlockAmbassadorsFromAdminRewardEndpoints() throws Exception {
        mockMvc.perform(get("/api/admin/revenue/rewards"))
                .andExpect(status().isForbidden());

        verifyNoInteractions(revenueAdminService);
    }

    @Test
    @WithMockUser(roles = "AMBASSADOR")
    void shouldBlockAmbassadorsFromApprovingRewards() throws Exception {
        mockMvc.perform(post("/api/admin/revenue/rewards/1/approve"))
                .andExpect(status().isForbidden());

        verifyNoInteractions(revenueAdminService);
    }

    @Test
    @WithMockUser(roles = "COMPANY_ADMIN")
    void shouldAllowCompanyAdminToGetCampaignReport() throws Exception {
        when(revenueAdminService.getCampaignReport(11L)).thenReturn(new CampaignRevenueReportResponse(
                11L, "Campaign", 0, 0, 0, Map.of(), 0,
                java.math.BigDecimal.ZERO, java.math.BigDecimal.ZERO, java.math.BigDecimal.ZERO,
                java.math.BigDecimal.ZERO, java.math.BigDecimal.ZERO, java.math.BigDecimal.ZERO, List.of()));

        mockMvc.perform(get("/api/admin/revenue/campaigns/11/report"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "COMPANY")
    void shouldBlockApiKeyCompanyRoleFromAdminEndpoints() throws Exception {
        mockMvc.perform(get("/api/admin/revenue/rewards"))
                .andExpect(status().isForbidden());

        verifyNoInteractions(revenueAdminService);
    }

    @TestConfiguration
    @EnableMethodSecurity
    static class MethodSecurityTestConfig {
    }
}
