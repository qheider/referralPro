package com.actpro.referral.security;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final ApiKeyAuthenticationFilter apiKeyAuthenticationFilter;
    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    @Value("${app.cors.allowed-origins}")
    private List<String> corsAllowedOrigins;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        // Public endpoints
                        .requestMatchers("/api/companies/register").permitAll()
                        .requestMatchers("/api/auth/login").permitAll()
                        .requestMatchers("/api/auth/hash").permitAll()
                        .requestMatchers("/api/auth/accept-invitation").permitAll()
                        .requestMatchers("/api/auth/verify-email").permitAll()
                        // Public ambassador application submission - no account exists yet for the
                        // applicant. Only the exact /apply path: everything else under
                        // /api/ambassador-applications/** is the /api/admin/... review surface,
                        // already covered by anyRequest().authenticated() + @PreAuthorize.
                        .requestMatchers("/api/ambassador-applications/apply").permitAll()
                        // Public referred-customer lead submission - the visitor has no account
                        // yet. Single-segment wildcard for the path token, not a broad /**.
                        .requestMatchers("/api/referral-links/*/leads").permitAll()
                        // Public campaign join-link resolution - the visitor has no account yet.
                        .requestMatchers("/api/campaigns/join/**").permitAll()
                        // Public inbound company webhooks - authenticated via HMAC signature
                        // (WebhookSignatureVerifier), not a bearer token. Single-segment wildcard
                        // for {companyCode}, not a broad /**.
                        .requestMatchers("/api/v1/integrations/*/webhooks/service-status").permitAll()
                        .requestMatchers("/r/**").permitAll()
                        .requestMatchers("/swagger-ui/**", "/v3/api-docs/**", "/swagger-ui.html").permitAll()
                        .requestMatchers("/actuator/**").permitAll()
                        // Dashboard endpoints - require JWT authentication
                        .requestMatchers("/api/dashboard/**").authenticated()
                        .requestMatchers("/api/auth/me").authenticated()
                        // Integration endpoints - require API key (ROLE_COMPANY); enforced by
                        // @PreAuthorize on each controller, not narrowed here
                        .requestMatchers("/api/referrals/**").authenticated()
                        .requestMatchers("/api/conversions/**").authenticated()
                        .requestMatchers("/api/rewards/**").authenticated()
                        // /api/companies/register is public (see above); every other path under
                        // /api/companies/** (e.g. CampaignController) is a JWT/COMPANY_ADMIN
                        // dashboard flow via CurrentUserService, not part of the API-key surface
                        .requestMatchers("/api/companies/**").authenticated()
                        // All other requests require authentication
                        .anyRequest().authenticated()
                )
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
                .addFilterBefore(apiKeyAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        // Use allowedOriginPatterns to support wildcards for local development; the default value
        // of app.cors.allowed-origins (see application.yml) preserves today's localhost/LAN/
        // Tailscale patterns exactly - real deployments override via CORS_ALLOWED_ORIGINS.
        // setAllowedOriginPatterns accepts exact URLs fine (patterns are a superset), so UAT/prod
        // origins don't need setAllowedOrigins.
        configuration.setAllowedOriginPatterns(corsAllowedOrigins);
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("*"));
        configuration.setAllowCredentials(true);
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}
