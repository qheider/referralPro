package com.actpro.referral.referral;

import com.actpro.referral.common.ApiResponse;
import com.actpro.referral.referral.dto.SubmitReferralLeadRequest;
import com.actpro.referral.referral.dto.SubmitReferralLeadResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Public - the referred customer has no account, so this is unauthenticated (see
 * SecurityConfig). Only the ambassador-link (ReferralLink/publicToken) model is supported; see
 * ReferralLeadService's class javadoc for why the legacy direct-API flow doesn't apply here.
 */
@Tag(name = "Referral Leads", description = "Public referred-customer lead submission")
@RestController
@RequestMapping("/api/referral-links")
@RequiredArgsConstructor
public class ReferralLeadController {

    private final ReferralLeadService referralLeadService;

    @Operation(
            summary = "Submit a referred-customer lead",
            description = "Public endpoint - creates the Referral for an ambassador referral-link click once " +
                    "the visitor identifies themselves. Idempotent within the same browsing session."
    )
    @PostMapping("/{token}/leads")
    public ResponseEntity<ApiResponse<SubmitReferralLeadResponse>> submitLead(
            @PathVariable String token,
            @CookieValue(name = AttributionSession.COOKIE_NAME, required = false) String rawSessionId,
            @Valid @RequestBody SubmitReferralLeadRequest request
    ) {
        String sessionId = AttributionSession.isValid(rawSessionId) ? rawSessionId : null;
        SubmitReferralLeadResponse response = referralLeadService.submitLead(token, sessionId, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Lead submitted successfully", response));
    }
}
