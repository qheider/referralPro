package com.actpro.referral.auth;

import com.actpro.referral.auth.dto.CurrentUserResponse;
import com.actpro.referral.auth.dto.LoginRequest;
import com.actpro.referral.auth.dto.LoginResponse;
import com.actpro.referral.common.ApiResponse;
import com.actpro.referral.security.CurrentUserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final CurrentUserService currentUserService;

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<LoginResponse>> login(@Valid @RequestBody LoginRequest request) {
        LoginResponse response = authService.login(request);
        return ResponseEntity.ok(
                new ApiResponse<>(true, "Login successful", response)
        );
    }

    @GetMapping("/me")
    @PreAuthorize("hasAnyRole('PLATFORM_ADMIN', 'COMPANY_ADMIN', 'AMBASSADOR')")
    public ResponseEntity<ApiResponse<CurrentUserResponse>> getCurrentUser() {
        CurrentUserResponse response = authService.getCurrentUser(currentUserService.getCurrentUserId());
        return ResponseEntity.ok(
                new ApiResponse<>(true, "User retrieved successfully", response)
        );
    }
    
    @GetMapping("/hash")
    public ResponseEntity<ApiResponse<String>> hashPassword(@RequestParam String password) {
        return ResponseEntity.ok(
                new ApiResponse<>(true, "Hash generated", authService.hashPassword(password))
        );
    }
}
