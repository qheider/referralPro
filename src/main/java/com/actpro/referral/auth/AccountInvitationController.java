package com.actpro.referral.auth;

import com.actpro.referral.ambassador.AmbassadorAdminService;
import com.actpro.referral.auth.dto.AcceptInvitationRequest;
import com.actpro.referral.auth.dto.AcceptInvitationResponse;
import com.actpro.referral.auth.dto.VerifyEmailRequest;
import com.actpro.referral.auth.dto.VerifyEmailResponse;
import com.actpro.referral.common.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Account Invitations", description = "Public one-time invitation acceptance")
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AccountInvitationController {

    private final AccountInvitationService accountInvitationService;
    private final AmbassadorAdminService ambassadorAdminService;

    @Operation(
            summary = "Accept an account invitation",
            description = "Public endpoint - sets the invited user's password and activates the account. " +
                    "The token is single-use and expires."
    )
    @PostMapping("/accept-invitation")
    public ResponseEntity<ApiResponse<AcceptInvitationResponse>> acceptInvitation(
            @Valid @RequestBody AcceptInvitationRequest request) {
        AcceptInvitationResponse response = accountInvitationService.acceptInvitation(request.token(), request.password());

        if ("AMBASSADOR".equals(response.role())) {
            ambassadorAdminService.activateInvitedAmbassador(response.userId());
        }

        return ResponseEntity.ok(ApiResponse.success("Invitation accepted successfully", response));
    }

    @Operation(
            summary = "Verify a company admin's email",
            description = "Public endpoint - activates the admin account created at company registration. " +
                    "The token is single-use and expires."
    )
    @PostMapping("/verify-email")
    public ResponseEntity<ApiResponse<VerifyEmailResponse>> verifyEmail(@Valid @RequestBody VerifyEmailRequest request) {
        DashboardUser user = accountInvitationService.verifyEmail(request.token());

        VerifyEmailResponse response = new VerifyEmailResponse(user.getId(), user.getUsername(), user.getCompany().getId());
        return ResponseEntity.ok(ApiResponse.success("Email verified successfully", response));
    }
}
