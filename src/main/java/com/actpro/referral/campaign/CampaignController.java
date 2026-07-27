package com.actpro.referral.campaign;

import com.actpro.referral.campaign.dto.CampaignResponse;
import com.actpro.referral.campaign.dto.CreateCampaignRequest;
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

    @Operation(summary = "Create a new campaign", description = "Create a new referral campaign for a company")
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

    private Long resolveCompanyId(Long companyId) {
        currentUserService.assertCurrentCompanyAccess(companyId);
        return currentUserService.getCurrentCompanyId();
    }
}
