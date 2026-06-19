package com.actpro.referral.security;

import com.actpro.referral.common.ErrorResponse;
import com.actpro.referral.company.Company;
import com.actpro.referral.company.CompanyRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.preauth.PreAuthenticatedAuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Optional;

@Slf4j
@Component
@RequiredArgsConstructor
public class ApiKeyAuthenticationFilter extends OncePerRequestFilter {

    private final CompanyRepository companyRepository;
    private final CompanyContext companyContext;
    private final ObjectMapper objectMapper;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        String path = request.getRequestURI();
        log.debug("Processing request: {} {}", request.getMethod(), path);

        // Skip filter for public endpoints
        if (isPublicEndpoint(path)) {
            log.debug("Public endpoint, skipping API key authentication");
            filterChain.doFilter(request, response);
            return;
        }

        // Extract API key from Authorization header
        String authHeader = request.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("ApiKey ")) {
            log.warn("Missing or invalid Authorization header for path: {}", path);
            sendUnauthorizedResponse(response, "Missing or invalid API key");
            return;
        }

        String apiKey = authHeader.substring(7); // Remove "ApiKey " prefix
        log.debug("Extracted API key: {}...", apiKey.substring(0, Math.min(10, apiKey.length())));

        // Find company by API key
        Optional<Company> companyOpt = companyRepository.findByApiKey(apiKey);
        if (companyOpt.isEmpty()) {
            log.warn("Invalid API key: {}", apiKey);
            sendUnauthorizedResponse(response, "Invalid API key");
            return;
        }

        Company company = companyOpt.get();
        log.info("Authenticated company: {} (ID: {})", company.getName(), company.getId());

        // Store company in context
        companyContext.setCurrentCompany(company);

        // Create Spring Security authentication
        PreAuthenticatedAuthenticationToken authentication =
                new PreAuthenticatedAuthenticationToken(
                        company.getId(),
                        apiKey,
                        Collections.singletonList(new SimpleGrantedAuthority("ROLE_COMPANY"))
                );
        authentication.setAuthenticated(true);
        SecurityContextHolder.getContext().setAuthentication(authentication);
        log.debug("Set authentication in SecurityContext: {}", authentication);

        try {
            filterChain.doFilter(request, response);
        } finally {
            // Clear context after request
            companyContext.clear();
            SecurityContextHolder.clearContext();
        }
    }

    private boolean isPublicEndpoint(String path) {
        return path.startsWith("/api/companies/register") ||
                path.startsWith("/r/") ||
                path.startsWith("/swagger-ui") ||
                path.startsWith("/v3/api-docs") ||
                path.startsWith("/actuator");
    }

    private void sendUnauthorizedResponse(HttpServletResponse response, String message) throws IOException {
        ErrorResponse errorResponse = new ErrorResponse(
                LocalDateTime.now(),
                HttpStatus.UNAUTHORIZED.value(),
                "Unauthorized",
                message,
                ""
        );

        response.setStatus(HttpStatus.UNAUTHORIZED.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.getWriter().write(objectMapper.writeValueAsString(errorResponse));
    }
}
