package com.actpro.referral.revenue;

import com.actpro.referral.common.ApiResponse;
import com.actpro.referral.revenue.dto.AmbassadorRewardResponse;
import com.actpro.referral.revenue.dto.CampaignRevenueReportResponse;
import com.actpro.referral.revenue.dto.RejectRewardRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Company-admin revenue/reward monitoring and the manual ELIGIBLE -> APPROVED -> PAID / REJECTED
 * lifecycle. Every method resolves the caller's company via {@code CurrentUserService}, same
 * no-path-variable pattern as {@code CompanyIntegrationAdminController}.
 */
@Tag(name = "Revenue and Rewards", description = "Ambassador-attribution revenue events and reward payout lifecycle")
@RestController
@RequestMapping("/api/admin/revenue")
@RequiredArgsConstructor
@PreAuthorize("hasRole('COMPANY_ADMIN')")
public class RevenueAdminController {

    private final RevenueAdminService revenueAdminService;

    @Operation(summary = "List ambassador rewards", description = "Optionally filtered by campaign and/or status")
    @GetMapping("/rewards")
    public ResponseEntity<ApiResponse<List<AmbassadorRewardResponse>>> listRewards(
            @RequestParam(required = false) Long campaignId,
            @RequestParam(required = false) AmbassadorRewardStatus status,
            @RequestParam(required = false, defaultValue = "50") int limit) {
        return ResponseEntity.ok(ApiResponse.success(revenueAdminService.listRewards(campaignId, status, limit)));
    }

    @Operation(summary = "Get a single reward")
    @GetMapping("/rewards/{rewardId}")
    public ResponseEntity<ApiResponse<AmbassadorRewardResponse>> getReward(@PathVariable Long rewardId) {
        return ResponseEntity.ok(ApiResponse.success(revenueAdminService.getReward(rewardId)));
    }

    @Operation(summary = "Approve an ELIGIBLE reward for payout")
    @PostMapping("/rewards/{rewardId}/approve")
    public ResponseEntity<ApiResponse<AmbassadorRewardResponse>> approve(@PathVariable Long rewardId) {
        return ResponseEntity.ok(ApiResponse.success("Reward approved", revenueAdminService.approve(rewardId)));
    }

    @Operation(summary = "Mark an APPROVED reward as paid", description = "Manual payout tracking only - no payment gateway integration")
    @PostMapping("/rewards/{rewardId}/mark-paid")
    public ResponseEntity<ApiResponse<AmbassadorRewardResponse>> markPaid(@PathVariable Long rewardId) {
        return ResponseEntity.ok(ApiResponse.success("Reward marked as paid", revenueAdminService.markPaid(rewardId)));
    }

    @Operation(summary = "Reject a reward", description = "Allowed from any pre-PAID status")
    @PostMapping("/rewards/{rewardId}/reject")
    public ResponseEntity<ApiResponse<AmbassadorRewardResponse>> reject(
            @PathVariable Long rewardId, @Valid @RequestBody RejectRewardRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Reward rejected", revenueAdminService.reject(rewardId, request.reason())));
    }

    @Operation(summary = "Get a campaign's revenue/reward report", description = "Qualifying/reversed event counts, revenue by currency, reward totals by status, and an ambassador leaderboard")
    @GetMapping("/campaigns/{campaignId}/report")
    public ResponseEntity<ApiResponse<CampaignRevenueReportResponse>> getCampaignReport(@PathVariable Long campaignId) {
        return ResponseEntity.ok(ApiResponse.success(revenueAdminService.getCampaignReport(campaignId)));
    }
}
