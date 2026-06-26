package com.actpro.referral.auth;

import com.actpro.referral.auth.dto.CurrentUserResponse;
import com.actpro.referral.auth.dto.LoginRequest;
import com.actpro.referral.auth.dto.LoginResponse;
import com.actpro.referral.common.exception.UnauthorizedException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final DashboardUserRepository dashboardUserRepository;
    private final JwtTokenProvider jwtTokenProvider;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder(10);

    @Transactional(readOnly = true)
    public LoginResponse login(LoginRequest request) {
        DashboardUser user = dashboardUserRepository.findByUsernameWithCompany(request.getUsername())
                .orElseThrow(() -> new UnauthorizedException("Invalid username or password"));

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new UnauthorizedException("Invalid username or password");
        }

        String token = jwtTokenProvider.generateToken(
                user.getId(),
                user.getUsername(),
                user.getCompany().getId(),
                user.getRole()
        );

        return new LoginResponse(
                token,
                user.getId(),
                user.getUsername(),
                user.getCompany().getId(),
                user.getCompany().getName(),
                user.getRole()
        );
    }

    @Transactional(readOnly = true)
    public CurrentUserResponse getCurrentUser(Long userId) {
        DashboardUser user = dashboardUserRepository.findById(userId)
                .orElseThrow(() -> new UnauthorizedException("User not found"));

        return new CurrentUserResponse(
                user.getId(),
                user.getUsername(),
                user.getCompany().getId(),
                user.getCompany().getName(),
                user.getRole()
        );
    }

    public String hashPassword(String plainPassword) {
        return passwordEncoder.encode(plainPassword);
    }
}
