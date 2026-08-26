package com.actpro.referral.auth;

import com.actpro.referral.auth.dto.ForgotPasswordRequest;
import com.actpro.referral.auth.dto.ResetPasswordRequest;
import com.actpro.referral.auth.dto.ResetPasswordResponse;
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

@Tag(name = "Password Reset", description = "Public \"forgot password\" flow for dashboard users")
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class PasswordResetController {

    private static final String GENERIC_REQUEST_MESSAGE =
            "If an account with that email exists, we've sent a password reset link.";

    private final PasswordResetService passwordResetService;

    @Operation(
            summary = "Request a password reset link",
            description = "Public endpoint - always returns a generic success message, whether or not the " +
                    "email belongs to an account, so the response can't be used to enumerate accounts."
    )
    @PostMapping("/forgot-password")
    public ResponseEntity<ApiResponse<Void>> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        passwordResetService.requestReset(request.email());
        return ResponseEntity.ok(ApiResponse.success(GENERIC_REQUEST_MESSAGE, null));
    }

    @Operation(
            summary = "Reset a password using a reset link token",
            description = "Public endpoint - sets a new password for the account tied to the token. " +
                    "The token is single-use and expires."
    )
    @PostMapping("/reset-password")
    public ResponseEntity<ApiResponse<ResetPasswordResponse>> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        PasswordResetService.ResetOutcome outcome = passwordResetService.resetPassword(request.token(), request.newPassword());
        ResetPasswordResponse response = new ResetPasswordResponse(outcome.userId(), outcome.username());
        return ResponseEntity.ok(ApiResponse.success("Password reset successfully", response));
    }
}
