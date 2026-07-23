package com.actpro.referral.security;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final ApiKeyAuthenticationFilter apiKeyAuthenticationFilter;
    private final JwtAuthenticationFilter jwtAuthenticationFilter;

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
                        .requestMatchers("/r/**").permitAll()
                        .requestMatchers("/swagger-ui/**", "/v3/api-docs/**", "/swagger-ui.html").permitAll()
                        .requestMatchers("/actuator/**").permitAll()
                        // Dashboard endpoints - require JWT authentication
                        .requestMatchers("/api/dashboard/**").authenticated()
                        .requestMatchers("/api/auth/me").authenticated()
                        // Protected endpoints - require API key
                        .requestMatchers("/api/referrals/**").authenticated()
                        .requestMatchers("/api/conversions/**").authenticated()
                        .requestMatchers("/api/rewards/**").authenticated()
                        .requestMatchers("/api/companies/**").authenticated()
                        // All other requests require authentication
                        .anyRequest().authenticated()
                )
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
                .addFilterBefore(apiKeyAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        // Use allowedOriginPatterns to support wildcards for local development
        configuration.setAllowedOriginPatterns(List.of(
            "http://localhost",           // Docker frontend (port 80)
            "http://localhost:*",         // Any localhost port
            "http://127.0.0.1",           // Localhost IP (port 80)
            "http://127.0.0.1:*",         // Localhost IP (any port)
            "http://192.168.*.*",         // Local network (192.168.x.x)
            "http://192.168.*.*:*",       // Local network with port
            "http://10.*.*.*",            // Local network (10.x.x.x)
            "http://10.*.*.*:*",          // Local network with port
            "http://172.16.*.*",          // Local network (172.16-31.x.x)
            "http://172.16.*.*:*",         // Local network with port
            "http://100.93.215.82",         // Local tailscale network 
            "http://100.93.215.82:*",         // Local tailscale network with port
            "http://100.122.180.92",
            "http://100.122.180.92:*"      // tailscale network with port for basement desktop
        ));
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("*"));
        configuration.setAllowCredentials(true);
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}
