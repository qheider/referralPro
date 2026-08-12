package com.actpro.referral.integration.webhook;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import org.springframework.beans.factory.annotation.Autowired;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Real-filter-chain reachability check - the class of gap that silently broke two earlier
 * "complete" phases (Phase 2's /verify-email/apply/leads endpoints, Phase 3's /campaigns/join/**):
 * a public path registered in SecurityConfig's permitAll() but missed in
 * ApiKeyAuthenticationFilter's separate hardcoded allowlist gets a 401 from the filter before
 * Spring Security's permitAll ever applies - and every other *SecurityTest in this codebase uses
 * addFilters=false, which can never catch that. This test boots the real filter chain instead
 * (mirrors CompanyApiKeyRepositoryTest's @ActiveProfiles("test") pattern, the only existing
 * precedent for a full-context test against the H2 test profile).
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc // deliberately NOT addFilters=false - that's the entire point of this test
@ActiveProfiles("test")
class WebhookPublicPathSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void shouldReachWebhookEndpointWithoutAuthorizationHeader() throws Exception {
        // No Authorization header at all, and a companyCode that resolves to nothing real - the
        // assertion is NOT "200", it's "404 from WebhookIngestService's own company-resolution
        // step", which is only reachable by actually getting past both SecurityConfig and
        // ApiKeyAuthenticationFilter. A filter-level rejection would produce 401 instead.
        mockMvc.perform(post("/api/v1/integrations/does-not-exist/webhooks/service-status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isNotFound());
    }
}
