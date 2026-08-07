package com.actpro.referral.campaign;

import com.actpro.referral.campaign.dto.PublicCampaignResponse;
import com.actpro.referral.common.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Public - a prospective ambassador opening the published campaign link has no account yet, so
 * this is unauthenticated (see SecurityConfig and ApiKeyAuthenticationFilter's public-path list).
 * Distinct from CampaignController, which is the COMPANY_ADMIN-only management surface.
 */
@Tag(name = "Public Campaign", description = "Public campaign join-link resolution")
@RestController
@RequestMapping("/api/campaigns")
@RequiredArgsConstructor
public class PublicCampaignController {

    private final CampaignService campaignService;

    @Operation(
            summary = "Resolve a campaign join link",
            description = "Public endpoint - validates the campaign/company are active and ambassador enrollment is " +
                    "open. Used by the /join/{campaignCode} landing page before letting a visitor apply."
    )
    @GetMapping("/join/{campaignCode}")
    public ResponseEntity<ApiResponse<PublicCampaignResponse>> resolveJoinLink(@PathVariable String campaignCode) {
        PublicCampaignResponse response = campaignService.resolveJoinLink(campaignCode);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}
