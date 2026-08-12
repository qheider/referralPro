package com.actpro.referral.integration.webhook;

import com.actpro.referral.common.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Public, unauthenticated (by design - authenticated via HMAC signature instead) endpoint the
 * company's own systems call to report service-status changes. Registered as a public path in
 * BOTH {@code security.SecurityConfig} and {@code security.ApiKeyAuthenticationFilter} - the two
 * allowlists must be kept in sync (see the comment in ApiKeyAuthenticationFilter; this exact
 * class of gap silently broke two earlier "complete" phases).
 */
@Tag(name = "Webhooks", description = "Inbound company service-status webhooks")
@RestController
@RequestMapping("/api/v1/integrations")
@RequiredArgsConstructor
public class WebhookController {

    private final WebhookIngestService webhookIngestService;

    @Operation(
            summary = "Receive a company service-status webhook",
            description = "Authenticated via HMAC-SHA256 (X-Luup-Signature/X-Luup-Timestamp headers), not a bearer token. "
                    + "Durably stores the verified event and returns immediately - matching/status-mapping happens asynchronously."
    )
    @PostMapping(value = "/{companyCode}/webhooks/service-status", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ApiResponse<Void>> receiveServiceStatus(
            @PathVariable String companyCode,
            @RequestHeader(value = "X-Luup-Signature", required = false) String signature,
            @RequestHeader(value = "X-Luup-Timestamp", required = false) String timestamp,
            @RequestBody String rawBody) {
        webhookIngestService.ingest(companyCode, signature, timestamp, rawBody);
        return ResponseEntity.ok(ApiResponse.success("Accepted", null));
    }
}
