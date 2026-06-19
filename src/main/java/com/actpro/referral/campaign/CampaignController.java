package com.actpro.referral.campaign;

import com.actpro.referral.campaign.dto.CampaignResponse;
import com.actpro.referral.campaign.dto.CreateCampaignRequest;
import com.actpro.referral.common.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Campaign", description = "Campaign management APIs")
@RestController
@RequestMapping("/api/companies/{companyId}/campaigns")
@RequiredArgsConstructor
public class CampaignController {

    private final CampaignService campaignService;

    @Operation(summary = "Create a new campaign", description = "Create a new referral campaign for a company")
    @PostMapping
    public ResponseEntity<ApiResponse<CampaignResponse>> createCampaign(
            @PathVariable Long companyId,
            @Valid @RequestBody CreateCampaignRequest request) {
        CampaignResponse response = campaignService.createCampaign(companyId, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Campaign created successfully", response));
    }

    @Operation(summary = "Get all campaigns", description = "Get all campaigns for a company")
    @GetMapping
    public ResponseEntity<ApiResponse<List<CampaignResponse>>> getCampaigns(
            @PathVariable Long companyId) {
        List<CampaignResponse> campaigns = campaignService.getCampaignsByCompany(companyId);
        return ResponseEntity.ok(ApiResponse.success(campaigns));
    }

    @Operation(summary = "Get campaign by ID", description = "Get a specific campaign by ID")
    @GetMapping("/{campaignId}")
    public ResponseEntity<ApiResponse<CampaignResponse>> getCampaignById(
            @PathVariable Long companyId,
            @PathVariable Long campaignId) {
        CampaignResponse campaign = campaignService.getCampaignById(companyId, campaignId);
        return ResponseEntity.ok(ApiResponse.success(campaign));
    }
}
