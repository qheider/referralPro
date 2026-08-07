package com.actpro.referral.integration.webhook;

import com.actpro.referral.common.exception.BadRequestException;
import com.actpro.referral.common.exception.NotFoundException;
import com.actpro.referral.common.exception.UnauthorizedException;
import com.actpro.referral.security.ApiKeyAuthenticationFilter;
import com.actpro.referral.security.JwtAuthenticationFilter;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// Thin controller-contract test - status codes per rejection case, real security filter chain
// verification lives in WebhookPublicPathSecurityTest instead.
@WebMvcTest(WebhookController.class)
@AutoConfigureMockMvc(addFilters = false)
class WebhookControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private WebhookIngestService webhookIngestService;

    @MockBean
    private ApiKeyAuthenticationFilter apiKeyAuthenticationFilter;

    @MockBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    private static final String BODY = "{\"eventId\":\"evt_1\"}";

    @Test
    void shouldReturn200OnSuccessfulIngest() throws Exception {
        doNothing().when(webhookIngestService).ingest(eq("CODE1"), eq("sig"), eq("123"), eq(BODY));

        mockMvc.perform(post("/api/v1/integrations/CODE1/webhooks/service-status")
                        .header("X-Luup-Signature", "sig")
                        .header("X-Luup-Timestamp", "123")
                        .contentType("application/json")
                        .content(BODY))
                .andExpect(status().isOk());
    }

    @Test
    void shouldPassRawBodyThroughUnmodified() throws Exception {
        String preciseBody = "{\"eventId\":\"evt_1\",\"extra\":  \"  spaced  \"}";
        doNothing().when(webhookIngestService).ingest(any(), any(), any(), eq(preciseBody));

        mockMvc.perform(post("/api/v1/integrations/CODE1/webhooks/service-status")
                        .header("X-Luup-Signature", "sig")
                        .header("X-Luup-Timestamp", "123")
                        .contentType("application/json")
                        .content(preciseBody))
                .andExpect(status().isOk());

        verify(webhookIngestService).ingest(eq("CODE1"), eq("sig"), eq("123"), eq(preciseBody));
    }

    @Test
    void shouldReturn404ForUnknownCompanyCode() throws Exception {
        doThrow(new NotFoundException("Unknown webhook endpoint")).when(webhookIngestService).ingest(any(), any(), any(), any());

        mockMvc.perform(post("/api/v1/integrations/UNKNOWN/webhooks/service-status")
                        .contentType("application/json")
                        .content(BODY))
                .andExpect(status().isNotFound());
    }

    @Test
    void shouldReturn401ForInvalidSignature() throws Exception {
        doThrow(new UnauthorizedException("Invalid or stale webhook signature")).when(webhookIngestService).ingest(any(), any(), any(), any());

        mockMvc.perform(post("/api/v1/integrations/CODE1/webhooks/service-status")
                        .header("X-Luup-Signature", "bad")
                        .header("X-Luup-Timestamp", "123")
                        .contentType("application/json")
                        .content(BODY))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void shouldReturn400ForMalformedPayload() throws Exception {
        doThrow(new BadRequestException("Malformed webhook payload")).when(webhookIngestService).ingest(any(), any(), any(), any());

        mockMvc.perform(post("/api/v1/integrations/CODE1/webhooks/service-status")
                        .header("X-Luup-Signature", "sig")
                        .header("X-Luup-Timestamp", "123")
                        .contentType("application/json")
                        .content("not-json"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldAllowMissingHeadersAtTheHttpLayer() throws Exception {
        // The controller doesn't require the headers itself (required = false) - WebhookIngestService
        // is what rejects a missing signature, as a 401 UnauthorizedException.
        doThrow(new UnauthorizedException("Missing webhook signature headers"))
                .when(webhookIngestService).ingest(eq("CODE1"), isNull(), isNull(), eq(BODY));

        mockMvc.perform(post("/api/v1/integrations/CODE1/webhooks/service-status")
                        .contentType("application/json")
                        .content(BODY))
                .andExpect(status().isUnauthorized());
    }
}
