package com.actpro.referral.security;

import com.actpro.referral.auth.DashboardUser;
import com.actpro.referral.auth.DashboardUserRepository;
import com.actpro.referral.auth.JwtTokenProvider;
import com.actpro.referral.auth.UserStatus;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;

@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtTokenProvider jwtTokenProvider;
    private final DashboardUserRepository dashboardUserRepository;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        try {
            String jwt = extractJwtFromRequest(request);

            if (jwt != null && jwtTokenProvider.validateToken(jwt)) {
                Long userId = jwtTokenProvider.getUserIdFromToken(jwt);
                Long companyId = jwtTokenProvider.getCompanyIdFromToken(jwt);
                String username = jwtTokenProvider.getUsernameFromToken(jwt);
                String role = jwtTokenProvider.getRoleFromToken(jwt);

                DashboardUser user = dashboardUserRepository.findByIdWithCompany(userId).orElse(null);

                if (user != null
                        && user.getStatus() == UserStatus.ACTIVE
                        && user.getCompany() != null
                        && user.getCompany().getId().equals(companyId)
                        && user.getUsername().equals(username)
                        && user.getRole().name().equals(role)) {
                    // Set company context for multi-tenancy
                    CompanyContext.setCurrentCompany(user.getCompany());

                    // Create authentication token
                    UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                            new AuthenticatedUser(
                                    user.getId(),
                                    user.getUsername(),
                                    user.getCompany().getId(),
                                    user.getRole()
                            ),
                            null,
                            Collections.singletonList(new SimpleGrantedAuthority("ROLE_" + user.getRole().name()))
                    );
                    authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

                    SecurityContextHolder.getContext().setAuthentication(authentication);
                }
            }
        } catch (Exception ex) {
            logger.error("Could not set user authentication in security context", ex);
        } finally {
            filterChain.doFilter(request, response);
            // Clear context after request
            CompanyContext.clear();
            SecurityContextHolder.clearContext();
        }
    }

    private String extractJwtFromRequest(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
    }
}
