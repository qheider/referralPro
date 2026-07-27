package com.actpro.referral.campaign;

import com.actpro.referral.campaign.dto.CampaignResponse;
import com.actpro.referral.security.CurrentUserService;
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

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(CampaignController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(CampaignControllerSecurityTest.MethodSecurityTestConfig.class)
class CampaignControllerSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private CampaignService campaignService;

    @MockBean
    private CurrentUserService currentUserService;

    @MockBean
    private ApiKeyAuthenticationFilter apiKeyAuthenticationFilter;

    @MockBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @Test
    @WithMockUser(roles = "COMPANY_ADMIN")
    void shouldUseAuthenticatedCompanyScopeInsteadOfRequestedPathValue() throws Exception {
        doNothing().when(currentUserService).assertCurrentCompanyAccess(123L);
        when(currentUserService.getCurrentCompanyId()).thenReturn(99L);
        when(campaignService.getCampaignsByCompany(99L)).thenReturn(List.<CampaignResponse>of());

        mockMvc.perform(get("/api/companies/123/campaigns"))
                .andExpect(status().isOk());

        verify(currentUserService).assertCurrentCompanyAccess(123L);
        verify(campaignService).getCampaignsByCompany(eq(99L));
    }

    @Test
    @WithMockUser(roles = "AMBASSADOR")
    void shouldBlockAmbassadorsFromAdminCampaignEndpoints() throws Exception {
        mockMvc.perform(get("/api/companies/123/campaigns"))
                .andExpect(status().isForbidden());

        verifyNoInteractions(currentUserService, campaignService);
    }

    @TestConfiguration
    @EnableMethodSecurity
    static class MethodSecurityTestConfig {
    }
}
