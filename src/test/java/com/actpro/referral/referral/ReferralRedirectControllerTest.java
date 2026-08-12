package com.actpro.referral.referral;

import com.actpro.referral.click.ReferralClickService;
import com.actpro.referral.security.ApiKeyAuthenticationFilter;
import com.actpro.referral.security.CurrentUserService;
import com.actpro.referral.security.JwtAuthenticationFilter;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.matchesPattern;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.ArgumentMatchers.matches;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.cookie;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ReferralRedirectController.class)
@AutoConfigureMockMvc(addFilters = false)
class ReferralRedirectControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ReferralClickService referralClickService;

    @MockBean
    private CurrentUserService currentUserService;

    @MockBean
    private ApiKeyAuthenticationFilter apiKeyAuthenticationFilter;

    @MockBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @Test
    void shouldIssueNewAttributionCookieWhenNoneSupplied() throws Exception {
        when(referralClickService.resolveAndRecordClick(eq("AbcDef1234567890"), any(), any(), any(), matches("[0-9a-f-]{36}")))
                .thenReturn("https://campaign.example.com?ref=AbcDef1234567890");

        mockMvc.perform(get("/r/AbcDef1234567890"))
                .andExpect(status().is3xxRedirection())
                .andExpect(header().string("Location", "https://campaign.example.com?ref=AbcDef1234567890"))
                .andExpect(cookie().exists("rp_attr_session"))
                .andExpect(cookie().value("rp_attr_session", matchesPattern("[0-9a-f-]{36}")))
                .andExpect(cookie().httpOnly("rp_attr_session", true))
                .andExpect(cookie().path("rp_attr_session", "/"))
                .andExpect(cookie().maxAge("rp_attr_session", (int) java.time.Duration.ofDays(30).toSeconds()));
    }

    @Test
    void shouldReuseExistingAttributionCookieWithoutSettingANewOne() throws Exception {
        when(referralClickService.resolveAndRecordClick(eq("AbcDef1234567890"), any(), any(), isNull(), eq("existing-session-id")))
                .thenReturn("https://campaign.example.com?ref=AbcDef1234567890");

        mockMvc.perform(get("/r/AbcDef1234567890").cookie(new Cookie("rp_attr_session", "existing-session-id")))
                .andExpect(status().is3xxRedirection())
                .andExpect(header().string("Location", "https://campaign.example.com?ref=AbcDef1234567890"))
                .andExpect(header().doesNotExist("Set-Cookie"));
    }

    @Test
    void shouldIgnoreOversizedOrInvalidCookieValueAndIssueAFreshOne() throws Exception {
        String oversizedValue = "a".repeat(200);

        when(referralClickService.resolveAndRecordClick(eq("AbcDef1234567890"), any(), any(), any(), matches("[0-9a-f-]{36}")))
                .thenReturn("https://campaign.example.com?ref=AbcDef1234567890");

        mockMvc.perform(get("/r/AbcDef1234567890").cookie(new Cookie("rp_attr_session", oversizedValue)))
                .andExpect(status().is3xxRedirection())
                .andExpect(cookie().exists("rp_attr_session"));
    }
}
