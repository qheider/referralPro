package com.actpro.referral.ai;

import com.actpro.referral.ai.dto.AskDashboardRequest;
import com.actpro.referral.ai.dto.AskDashboardResponse;
import com.actpro.referral.common.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/ai")
@RequiredArgsConstructor
@PreAuthorize("hasRole('COMPANY_ADMIN')")
public class AiController {

    private final DashboardCopilotService dashboardCopilotService;

    @PostMapping("/dashboard/ask")
    public ResponseEntity<ApiResponse<AskDashboardResponse>> askDashboard(@Valid @RequestBody AskDashboardRequest request) {
        AskDashboardResponse response = dashboardCopilotService.ask(request.question(), request.campaignId());
        return ResponseEntity.ok(new ApiResponse<>(true, "Dashboard copilot answered successfully", response));
    }
}
