package com.actpro.referral.campaign;

import com.actpro.referral.campaign.dto.CampaignResponse;
import com.actpro.referral.campaign.dto.CreateCampaignRequest;
import com.actpro.referral.campaign.dto.UpdateCampaignRequest;
import com.actpro.referral.common.ApiResponse;
import com.actpro.referral.security.CurrentUserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Campaign", description = "Campaign management APIs")
@RestController
@RequestMapping("/api/companies/{companyId}/campaigns")
@RequiredArgsConstructor
@PreAuthorize("hasRole('COMPANY_ADMIN')")
public class CampaignController {

    private final CampaignService campaignService;
    private final CurrentUserService currentUserService;

    @Operation(summary = "Create a new campaign", description = "Create a new referral campaign for a company, starting in DRAFT status")
    @PostMapping
    public ResponseEntity<ApiResponse<CampaignResponse>> createCampaign(
            @PathVariable Long companyId,
            @Valid @RequestBody CreateCampaignRequest request) {
        Long currentCompanyId = resolveCompanyId(companyId);
        CampaignResponse response = campaignService.createCampaign(currentCompanyId, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Campaign created successfully", response));
    }

    @Operation(summary = "Get all campaigns", description = "Get all campaigns for a company")
    @GetMapping
    public ResponseEntity<ApiResponse<List<CampaignResponse>>> getCampaigns(
            @PathVariable Long companyId) {
        Long currentCompanyId = resolveCompanyId(companyId);
        List<CampaignResponse> campaigns = campaignService.getCampaignsByCompany(currentCompanyId);
        return ResponseEntity.ok(ApiResponse.success(campaigns));
    }

    @Operation(summary = "Get campaign by ID", description = "Get a specific campaign by ID")
    @GetMapping("/{campaignId}")
    public ResponseEntity<ApiResponse<CampaignResponse>> getCampaignById(
            @PathVariable Long companyId,
            @PathVariable Long campaignId) {
        Long currentCompanyId = resolveCompanyId(companyId);
        CampaignResponse campaign = campaignService.getCampaignById(currentCompanyId, campaignId);
        return ResponseEntity.ok(ApiResponse.success(campaign));
    }

    @Operation(summary = "Update a campaign", description = "Partial update - reward terms and start date are locked once the campaign leaves DRAFT")
    @PutMapping("/{campaignId}")
    public ResponseEntity<ApiResponse<CampaignResponse>> updateCampaign(
            @PathVariable Long companyId,
            @PathVariable Long campaignId,
            @Valid @RequestBody UpdateCampaignRequest request) {
        Long currentCompanyId = resolveCompanyId(companyId);
        CampaignResponse response = campaignService.updateCampaign(currentCompanyId, campaignId, request);
        return ResponseEntity.ok(ApiResponse.success("Campaign updated successfully", response));
    }

    @Operation(summary = "Publish a campaign", description = "DRAFT -> SCHEDULED or ACTIVE, depending on the start date")
    @PostMapping("/{campaignId}/publish")
    public ResponseEntity<ApiResponse<CampaignResponse>> publishCampaign(
            @PathVariable Long companyId,
            @PathVariable Long campaignId) {
        Long currentCompanyId = resolveCompanyId(companyId);
        CampaignResponse response = campaignService.publishCampaign(currentCompanyId, campaignId);
        return ResponseEntity.ok(ApiResponse.success("Campaign published successfully", response));
    }

    @Operation(summary = "Pause a campaign", description = "ACTIVE -> PAUSED")
    @PostMapping("/{campaignId}/pause")
    public ResponseEntity<ApiResponse<CampaignResponse>> pauseCampaign(
            @PathVariable Long companyId,
            @PathVariable Long campaignId) {
        Long currentCompanyId = resolveCompanyId(companyId);
        CampaignResponse response = campaignService.pauseCampaign(currentCompanyId, campaignId);
        return ResponseEntity.ok(ApiResponse.success("Campaign paused successfully", response));
    }

    @Operation(summary = "Resume a paused campaign", description = "PAUSED -> ACTIVE")
    @PostMapping("/{campaignId}/resume")
    public ResponseEntity<ApiResponse<CampaignResponse>> resumeCampaign(
            @PathVariable Long companyId,
            @PathVariable Long campaignId) {
        Long currentCompanyId = resolveCompanyId(companyId);
        CampaignResponse response = campaignService.resumeCampaign(currentCompanyId, campaignId);
        return ResponseEntity.ok(ApiResponse.success("Campaign resumed successfully", response));
    }

    @Operation(summary = "Close a campaign", description = "SCHEDULED/ACTIVE/PAUSED -> CLOSED (admin-initiated, permanent)")
    @PostMapping("/{campaignId}/close")
    public ResponseEntity<ApiResponse<CampaignResponse>> closeCampaign(
            @PathVariable Long companyId,
            @PathVariable Long campaignId) {
        Long currentCompanyId = resolveCompanyId(companyId);
        CampaignResponse response = campaignService.closeCampaign(currentCompanyId, campaignId);
        return ResponseEntity.ok(ApiResponse.success("Campaign closed successfully", response));
    }

    @Operation(summary = "Archive a campaign", description = "EXPIRED/CLOSED -> ARCHIVED")
    @PostMapping("/{campaignId}/archive")
    public ResponseEntity<ApiResponse<CampaignResponse>> archiveCampaign(
            @PathVariable Long companyId,
            @PathVariable Long campaignId) {
        Long currentCompanyId = resolveCompanyId(companyId);
        CampaignResponse response = campaignService.archiveCampaign(currentCompanyId, campaignId);
        return ResponseEntity.ok(ApiResponse.success("Campaign archived successfully", response));
    }

    private Long resolveCompanyId(Long companyId) {
        currentUserService.assertCurrentCompanyAccess(companyId);
        return currentUserService.getCurrentCompanyId();
    }
}
