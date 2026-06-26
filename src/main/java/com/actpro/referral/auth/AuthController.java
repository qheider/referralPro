package com.actpro.referral.auth;

import com.actpro.referral.auth.dto.CurrentUserResponse;
import com.actpro.referral.auth.dto.LoginRequest;
import com.actpro.referral.auth.dto.LoginResponse;
import com.actpro.referral.common.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<LoginResponse>> login(@Valid @RequestBody LoginRequest request) {
        LoginResponse response = authService.login(request);
        return ResponseEntity.ok(
                new ApiResponse<>(true, "Login successful", response)
        );
    }

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<CurrentUserResponse>> getCurrentUser(Authentication authentication) {
        Long userId = (Long) authentication.getPrincipal();
        CurrentUserResponse response = authService.getCurrentUser(userId);
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
