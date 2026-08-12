package com.actpro.referral.ambassador;

import com.actpro.referral.ambassador.dto.AmbassadorRegistrationResponse;
import com.actpro.referral.ambassador.dto.SubmitAmbassadorApplicationRequest;
import com.actpro.referral.common.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Public - a prospective ambassador has no account yet, so this is unauthenticated (see
 * SecurityConfig). Unlike AmbassadorApplicationController's /apply path (admin-reviewed), this
 * provisions the ambassador account immediately - see
 * AmbassadorApplicationService.registerAmbassador's javadoc.
 */
@RestController
@RequestMapping("/api/ambassador-registrations")
@RequiredArgsConstructor
public class AmbassadorRegistrationController {

    private final AmbassadorApplicationService ambassadorApplicationService;

    @PostMapping
    public ResponseEntity<ApiResponse<AmbassadorRegistrationResponse>> register(
            @RequestParam Long companyId,
            @RequestParam(required = false) String campaignCode,
            @Valid @RequestBody SubmitAmbassadorApplicationRequest request
    ) {
        AmbassadorRegistrationResponse response =
                ambassadorApplicationService.registerAmbassador(companyId, campaignCode, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Registration submitted - check your email to activate your account", response));
    }
}
