package com.actpro.referral.conversion;

import com.actpro.referral.conversion.dto.ConversionRequest;
import com.actpro.referral.reward.Reward;
import com.actpro.referral.reward.RewardStatus;
import com.actpro.referral.reward.dto.RewardResult;
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

import java.math.BigDecimal;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ConversionController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(ConversionControllerSecurityTest.MethodSecurityTestConfig.class)
class ConversionControllerSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private ConversionService conversionService;

    @MockBean
    private ApiKeyAuthenticationFilter apiKeyAuthenticationFilter;

    @MockBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    private static final ConversionRequest REQUEST =
            new ConversionRequest("CODE123", "ext-user-1", "user@example.com", "User", "purchase");

    @Test
    @WithMockUser(roles = "COMPANY")
    void shouldAllowApiKeyAuthenticatedCompanyCallers() throws Exception {
        Conversion conversion = new Conversion();
        conversion.setId(1L);
        conversion.setStatus(ConversionStatus.COMPLETED);

        Reward referrerReward = reward("REF1");
        Reward refereeReward = reward("REF2");
        RewardResult rewardResult = new RewardResult(referrerReward, refereeReward);

        when(conversionService.completeConversion(any()))
                .thenReturn(new ConversionService.ConversionWithRewards(conversion, rewardResult));

        mockMvc.perform(post("/api/conversions")
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(REQUEST)))
                .andExpect(status().isCreated());
    }

    @Test
    @WithMockUser(roles = "AMBASSADOR")
    void shouldBlockAmbassadorJwtCallersFromIntegrationEndpoint() throws Exception {
        mockMvc.perform(post("/api/conversions")
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(REQUEST)))
                .andExpect(status().isForbidden());

        verifyNoInteractions(conversionService);
    }

    @Test
    @WithMockUser(roles = "COMPANY_ADMIN")
    void shouldBlockCompanyAdminJwtCallersFromIntegrationEndpoint() throws Exception {
        mockMvc.perform(post("/api/conversions")
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(REQUEST)))
                .andExpect(status().isForbidden());

        verifyNoInteractions(conversionService);
    }

    private Reward reward(String couponCode) {
        Reward reward = new Reward();
        reward.setCouponCode(couponCode);
        reward.setRewardValue(BigDecimal.TEN);
        reward.setStatus(RewardStatus.ISSUED);
        return reward;
    }

    @TestConfiguration
    @EnableMethodSecurity
    static class MethodSecurityTestConfig {
    }
}
