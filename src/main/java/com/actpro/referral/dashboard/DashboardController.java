package com.actpro.referral.dashboard;

import com.actpro.referral.common.ApiResponse;
import com.actpro.referral.dashboard.dto.*;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final DashboardService dashboardService;

    @GetMapping("/campaigns/overview")
    public ResponseEntity<ApiResponse<CampaignsOverviewResponse>> getCampaignsOverview() {
        CampaignsOverviewResponse response = dashboardService.getCampaignsOverview();
        return ResponseEntity.ok(
                new ApiResponse<>(true, "Campaigns overview retrieved successfully", response)
        );
    }

    @GetMapping("/campaigns/{campaignId}/stats")
    public ResponseEntity<ApiResponse<CampaignStatsResponse>> getCampaignStats(
            @PathVariable Long campaignId
    ) {
        CampaignStatsResponse response = dashboardService.getCampaignStats(campaignId);
        return ResponseEntity.ok(
                new ApiResponse<>(true, "Campaign stats retrieved successfully", response)
        );
    }

    @GetMapping("/campaigns/{campaignId}/funnel")
    public ResponseEntity<ApiResponse<ConversionFunnelResponse>> getConversionFunnel(
            @PathVariable Long campaignId
    ) {
        ConversionFunnelResponse response = dashboardService.getConversionFunnel(campaignId);
        return ResponseEntity.ok(
                new ApiResponse<>(true, "Conversion funnel retrieved successfully", response)
        );
    }

    @GetMapping("/campaigns/{campaignId}/top-referrers")
    public ResponseEntity<ApiResponse<TopReferrersResponse>> getTopReferrers(
            @PathVariable Long campaignId,
            @RequestParam(required = false, defaultValue = "10") Integer limit
    ) {
        TopReferrersResponse response = dashboardService.getTopReferrers(campaignId, limit);
        return ResponseEntity.ok(
                new ApiResponse<>(true, "Top referrers retrieved successfully", response)
        );
    }

    @GetMapping("/campaigns/{campaignId}/metrics/time-series")
    public ResponseEntity<ApiResponse<TimeSeriesResponse>> getTimeSeries(
            @PathVariable Long campaignId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false, defaultValue = "daily") String granularity
    ) {
        TimeSeriesResponse response = dashboardService.getTimeSeries(campaignId, startDate, endDate, granularity);
        return ResponseEntity.ok(
                new ApiResponse<>(true, "Time series metrics retrieved successfully", response)
        );
    }

    @GetMapping("/campaigns/{campaignId}/rewards/summary")
    public ResponseEntity<ApiResponse<RewardSummaryResponse>> getRewardSummary(
            @PathVariable Long campaignId
    ) {
        RewardSummaryResponse response = dashboardService.getRewardSummary(campaignId);
        return ResponseEntity.ok(
                new ApiResponse<>(true, "Reward summary retrieved successfully", response)
        );
    }
}
