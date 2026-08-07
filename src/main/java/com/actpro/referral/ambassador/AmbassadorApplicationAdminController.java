package com.actpro.referral.ambassador;

import com.actpro.referral.ambassador.dto.*;
import com.actpro.referral.common.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/ambassador-applications")
@RequiredArgsConstructor
@PreAuthorize("hasRole('COMPANY_ADMIN')")
public class AmbassadorApplicationAdminController {

    private final AmbassadorApplicationService ambassadorApplicationService;

    @GetMapping
    public ResponseEntity<ApiResponse<AmbassadorApplicationPageResponse>> listApplications(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String sort,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) ApplicationStatus status
    ) {
        AmbassadorApplicationPageResponse response = ambassadorApplicationService.listApplications(page, size, sort, search, status);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/{applicationId}")
    public ResponseEntity<ApiResponse<AmbassadorApplicationDetailResponse>> getApplication(@PathVariable Long applicationId) {
        AmbassadorApplicationDetailResponse response = ambassadorApplicationService.getApplication(applicationId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PostMapping("/{applicationId}/approve")
    public ResponseEntity<ApiResponse<AmbassadorApplicationApprovalResponse>> approveApplication(@PathVariable Long applicationId) {
        AmbassadorApplicationApprovalResponse response = ambassadorApplicationService.approveApplication(applicationId);
        return ResponseEntity.ok(ApiResponse.success("Application approved successfully", response));
    }

    @PostMapping("/{applicationId}/reject")
    public ResponseEntity<ApiResponse<AmbassadorApplicationDetailResponse>> rejectApplication(
            @PathVariable Long applicationId,
            @Valid @RequestBody RejectApplicationRequest request
    ) {
        AmbassadorApplicationDetailResponse response = ambassadorApplicationService.rejectApplication(applicationId, request);
        return ResponseEntity.ok(ApiResponse.success("Application rejected successfully", response));
    }
}
