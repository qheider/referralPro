package com.actpro.referral.referral;

import com.actpro.referral.common.ApiResponse;
import com.actpro.referral.referral.dto.GenerateReferralRequest;
import com.actpro.referral.referral.dto.GenerateReferralResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Referral", description = "Referral link generation and tracking APIs")
@RestController
@RequestMapping("/api/referrals")
@RequiredArgsConstructor
public class ReferralController {

    private final ReferralService referralService;

    @Operation(
            summary = "Generate referral link",
            description = "Generate a unique referral link for a customer to share with friends",
            security = @SecurityRequirement(name = "ApiKey")
    )
    @PostMapping("/generate")
    public ResponseEntity<ApiResponse<GenerateReferralResponse>> generateReferral(
            @Valid @RequestBody GenerateReferralRequest request) {
        GenerateReferralResponse response = referralService.generateReferral(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Referral link generated successfully", response));
    }
}
