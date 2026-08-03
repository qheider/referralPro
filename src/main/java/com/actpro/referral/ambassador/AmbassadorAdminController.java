package com.actpro.referral.ambassador;

import com.actpro.referral.ambassador.dto.*;
import com.actpro.referral.auth.dto.IssuedInvitationResponse;
import com.actpro.referral.common.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/ambassadors")
@RequiredArgsConstructor
@PreAuthorize("hasRole('COMPANY_ADMIN')")
public class AmbassadorAdminController {

    private final AmbassadorAdminService ambassadorAdminService;

    @PostMapping
    public ResponseEntity<ApiResponse<AmbassadorCreationResponse>> createAmbassador(
            @Valid @RequestBody CreateAmbassadorRequest request
    ) {
        AmbassadorCreationResponse response = ambassadorAdminService.createAmbassador(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Ambassador created successfully", response));
    }

    @PostMapping("/{ambassadorId}/resend-invitation")
    public ResponseEntity<ApiResponse<IssuedInvitationResponse>> resendInvitation(@PathVariable Long ambassadorId) {
        IssuedInvitationResponse response = ambassadorAdminService.resendInvitation(ambassadorId);
        return ResponseEntity.ok(ApiResponse.success("Invitation resent successfully", response));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<AmbassadorPageResponse>> listAmbassadors(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String sort,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) AmbassadorStatus status
    ) {
        AmbassadorPageResponse response = ambassadorAdminService.listAmbassadors(page, size, sort, search, status);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/{ambassadorId}")
    public ResponseEntity<ApiResponse<AmbassadorDetailResponse>> getAmbassador(@PathVariable Long ambassadorId) {
        AmbassadorDetailResponse response = ambassadorAdminService.getAmbassador(ambassadorId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PutMapping("/{ambassadorId}")
    public ResponseEntity<ApiResponse<AmbassadorDetailResponse>> updateAmbassador(
            @PathVariable Long ambassadorId,
            @Valid @RequestBody UpdateAmbassadorRequest request
    ) {
        AmbassadorDetailResponse response = ambassadorAdminService.updateAmbassador(ambassadorId, request);
        return ResponseEntity.ok(ApiResponse.success("Ambassador updated successfully", response));
    }

    @PatchMapping("/{ambassadorId}/activate")
    public ResponseEntity<ApiResponse<AmbassadorDetailResponse>> activateAmbassador(@PathVariable Long ambassadorId) {
        AmbassadorDetailResponse response = ambassadorAdminService.activateAmbassador(ambassadorId);
        return ResponseEntity.ok(ApiResponse.success("Ambassador activated successfully", response));
    }

    @PatchMapping("/{ambassadorId}/deactivate")
    public ResponseEntity<ApiResponse<AmbassadorDetailResponse>> deactivateAmbassador(@PathVariable Long ambassadorId) {
        AmbassadorDetailResponse response = ambassadorAdminService.deactivateAmbassador(ambassadorId);
        return ResponseEntity.ok(ApiResponse.success("Ambassador deactivated successfully", response));
    }
}
