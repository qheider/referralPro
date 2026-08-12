package com.actpro.referral.ambassador;

import com.actpro.referral.ambassador.dto.AmbassadorApplicationPageResponse;
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

import java.util.List;

import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AmbassadorApplicationAdminController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(AmbassadorApplicationAdminControllerSecurityTest.MethodSecurityTestConfig.class)
class AmbassadorApplicationAdminControllerSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AmbassadorApplicationService ambassadorApplicationService;

    @MockBean
    private CurrentUserService currentUserService;

    @MockBean
    private ApiKeyAuthenticationFilter apiKeyAuthenticationFilter;

    @MockBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @Test
    @WithMockUser(roles = "COMPANY_ADMIN")
    void shouldAllowCompanyAdminToListApplications() throws Exception {
        when(ambassadorApplicationService.listApplications(0, 20, null, null, null))
                .thenReturn(new AmbassadorApplicationPageResponse(List.of(), 0, 20, 0, 0, true, true));

        mockMvc.perform(get("/api/admin/ambassador-applications"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "AMBASSADOR")
    void shouldBlockAmbassadorsFromAdminApplicationEndpoints() throws Exception {
        mockMvc.perform(get("/api/admin/ambassador-applications"))
                .andExpect(status().isForbidden());

        verifyNoInteractions(ambassadorApplicationService);
    }

    @TestConfiguration
    @EnableMethodSecurity
    static class MethodSecurityTestConfig {
    }
}
