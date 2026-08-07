package com.actpro.referral.ambassador;

import com.actpro.referral.ambassador.dto.AmbassadorApplicationSubmissionResponse;
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
 * SecurityConfig). Admin review/approve/reject lives in AmbassadorApplicationAdminController.
 */
@RestController
@RequestMapping("/api/ambassador-applications")
@RequiredArgsConstructor
public class AmbassadorApplicationController {

    private final AmbassadorApplicationService ambassadorApplicationService;

    @PostMapping("/apply")
    public ResponseEntity<ApiResponse<AmbassadorApplicationSubmissionResponse>> apply(
            @RequestParam Long companyId,
            @Valid @RequestBody SubmitAmbassadorApplicationRequest request
    ) {
        AmbassadorApplicationSubmissionResponse response = ambassadorApplicationService.submitApplication(companyId, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Application submitted successfully", response));
    }
}
