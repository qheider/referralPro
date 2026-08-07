package com.actpro.referral.ambassador;

import com.actpro.referral.ambassador.dto.*;
import com.actpro.referral.common.ApiResponse;
import com.actpro.referral.referral.ReferralStatus;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/ambassador")
@RequiredArgsConstructor
@PreAuthorize("hasRole('AMBASSADOR')")
public class AmbassadorPortalController {

    private final AmbassadorPortalService ambassadorPortalService;

    @GetMapping("/dashboard")
    public ResponseEntity<ApiResponse<AmbassadorDashboardResponse>> getDashboard() {
        return ResponseEntity.ok(new ApiResponse<>(true, "Dashboard retrieved successfully", ambassadorPortalService.getDashboard()));
    }

    @GetMapping("/campaigns")
    public ResponseEntity<ApiResponse<List<AmbassadorCampaignOverviewResponse>>> listCampaigns() {
        return ResponseEntity.ok(new ApiResponse<>(true, "Campaigns retrieved successfully", ambassadorPortalService.listCampaigns()));
    }

    @GetMapping("/campaigns/{campaignId}")
    public ResponseEntity<ApiResponse<AmbassadorCampaignDetailResponse>> getCampaign(@PathVariable Long campaignId) {
        return ResponseEntity.ok(new ApiResponse<>(true, "Campaign retrieved successfully", ambassadorPortalService.getCampaign(campaignId)));
    }

    @GetMapping("/referral-links")
    public ResponseEntity<ApiResponse<List<ReferralLinkSummaryResponse>>> listReferralLinks() {
        return ResponseEntity.ok(new ApiResponse<>(true, "Referral links retrieved successfully", ambassadorPortalService.listReferralLinks()));
    }

    @GetMapping("/campaigns/{campaignId}/referral-link")
    public ResponseEntity<ApiResponse<ReferralLinkSummaryResponse>> getCampaignReferralLink(@PathVariable Long campaignId) {
        return ResponseEntity.ok(new ApiResponse<>(true, "Referral link retrieved successfully", ambassadorPortalService.getCampaignReferralLink(campaignId)));
    }

    @GetMapping("/referrals")
    public ResponseEntity<ApiResponse<AmbassadorReferralHistoryResponse>> listReferrals(
            @RequestParam(required = false) Long campaignId,
            @RequestParam(required = false) ReferralStatus status,
            @RequestParam(required = false) java.time.LocalDate fromDate,
            @RequestParam(required = false) java.time.LocalDate toDate,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return ResponseEntity.ok(new ApiResponse<>(
                true,
                "Referrals retrieved successfully",
                ambassadorPortalService.listReferrals(campaignId, status, fromDate, toDate, page, size)
        ));
    }

    @GetMapping("/analytics")
    public ResponseEntity<ApiResponse<AmbassadorAnalyticsResponse>> getAnalytics(
            @RequestParam(required = false) Long campaignId,
            @RequestParam(required = false) java.time.LocalDate fromDate,
            @RequestParam(required = false) java.time.LocalDate toDate
    ) {
        return ResponseEntity.ok(new ApiResponse<>(
                true,
                "Analytics retrieved successfully",
                ambassadorPortalService.getAnalytics(campaignId, fromDate, toDate)
        ));
    }

    @GetMapping("/earnings")
    public ResponseEntity<ApiResponse<AmbassadorEarningsSummaryResponse>> getEarningsSummary() {
        return ResponseEntity.ok(new ApiResponse<>(true, "Earnings summary retrieved successfully", ambassadorPortalService.getEarningsSummary()));
    }

    @GetMapping("/earnings/history")
    public ResponseEntity<ApiResponse<AmbassadorEarningsHistoryResponse>> listEarnings(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return ResponseEntity.ok(new ApiResponse<>(true, "Earnings history retrieved successfully", ambassadorPortalService.listEarnings(page, size)));
    }

    @GetMapping("/profile")
    public ResponseEntity<ApiResponse<AmbassadorProfileResponse>> getProfile() {
        return ResponseEntity.ok(new ApiResponse<>(true, "Profile retrieved successfully", ambassadorPortalService.getProfile()));
    }

    @PutMapping("/profile")
    public ResponseEntity<ApiResponse<AmbassadorProfileResponse>> updateProfile(@Valid @RequestBody UpdateAmbassadorProfileRequest request) {
        return ResponseEntity.ok(new ApiResponse<>(true, "Profile updated successfully", ambassadorPortalService.updateProfile(request)));
    }
}
