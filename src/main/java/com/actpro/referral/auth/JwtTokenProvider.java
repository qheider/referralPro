package com.actpro.referral.auth;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

@Component
public class JwtTokenProvider {

    // Phase 9 hardening fix: this used to read the plain "jwt.secret"/"jwt.expiration" property
    // paths, which nothing in application.yml ever sets - the real, env-var-driven properties are
    // "app.jwt.secret"/"app.jwt.expiration-minutes" (see application.yml). That meant every
    // deployment silently signed and verified tokens with the hardcoded fallback literal below,
    // regardless of the JWT_SECRET env var - a checked-in, guessable HS256 key that lets anyone
    // forge a JWT for any known username/companyId/role. Found by tenant-security-reviewer's
    // Phase 9 audit; fixed by binding to the property path JwtAuthenticationFilter/AuthService and
    // docker-compose.yml already assumed was in effect.
    @Value("${app.jwt.secret:your-super-secret-jwt-key-that-is-at-least-256-bits-long-for-hs256}")
    private String jwtSecret;

    @Value("${app.jwt.expiration-minutes:120}")
    private long jwtExpirationMinutes;

    private SecretKey getSigningKey() {
        return Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
    }

    public String generateToken(Long userId, String username, Long companyId, String role) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + java.time.Duration.ofMinutes(jwtExpirationMinutes).toMillis());

        return Jwts.builder()
                .subject(userId.toString())
                .claim("username", username)
                .claim("companyId", companyId)
                .claim("role", role)
                .issuedAt(now)
                .expiration(expiryDate)
                .signWith(getSigningKey())
                .compact();
    }

    public Long getUserIdFromToken(String token) {
        Claims claims = Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();

        return Long.parseLong(claims.getSubject());
    }

    public Long getCompanyIdFromToken(String token) {
        Claims claims = Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();

        return claims.get("companyId", Long.class);
    }

    public String getUsernameFromToken(String token) {
        Claims claims = Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();

        return claims.get("username", String.class);
    }

    public String getRoleFromToken(String token) {
        Claims claims = Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();

        return claims.get("role", String.class);
    }

    public boolean validateToken(String token) {
        try {
            Jwts.parser()
                    .verifyWith(getSigningKey())
                    .build()
                    .parseSignedClaims(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            // Log exception if needed
            return false;
        }
    }
}
