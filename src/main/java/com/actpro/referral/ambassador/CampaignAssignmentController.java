package com.actpro.referral.ambassador;

import com.actpro.referral.ambassador.dto.AssignAmbassadorsRequest;
import com.actpro.referral.ambassador.dto.AssignedCampaignResponse;
import com.actpro.referral.ambassador.dto.CampaignAssignmentResponse;
import com.actpro.referral.common.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
@PreAuthorize("hasRole('COMPANY_ADMIN')")
public class CampaignAssignmentController {

    private final CampaignAssignmentService campaignAssignmentService;

    @PostMapping("/campaigns/{campaignId}/ambassadors")
    public ResponseEntity<ApiResponse<List<CampaignAssignmentResponse>>> assignAmbassadors(
            @PathVariable Long campaignId,
            @Valid @RequestBody AssignAmbassadorsRequest request
    ) {
        List<CampaignAssignmentResponse> response = campaignAssignmentService.assignAmbassadors(campaignId, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Ambassadors assigned successfully", response));
    }

    @GetMapping("/campaigns/{campaignId}/ambassadors")
    public ResponseEntity<ApiResponse<List<CampaignAssignmentResponse>>> listCampaignAssignments(@PathVariable Long campaignId) {
        return ResponseEntity.ok(ApiResponse.success(campaignAssignmentService.listCampaignAssignments(campaignId)));
    }

    @DeleteMapping("/campaigns/{campaignId}/ambassadors/{ambassadorId}")
    public ResponseEntity<ApiResponse<Void>> removeCampaignAssignment(
            @PathVariable Long campaignId,
            @PathVariable Long ambassadorId
    ) {
        campaignAssignmentService.removeCampaignAssignment(campaignId, ambassadorId);
        return ResponseEntity.ok(ApiResponse.success("Campaign assignment removed successfully", null));
    }

    @GetMapping("/ambassadors/{ambassadorId}/campaigns")
    public ResponseEntity<ApiResponse<List<AssignedCampaignResponse>>> listAmbassadorCampaigns(@PathVariable Long ambassadorId) {
        return ResponseEntity.ok(ApiResponse.success(campaignAssignmentService.listAmbassadorCampaigns(ambassadorId)));
    }
}
