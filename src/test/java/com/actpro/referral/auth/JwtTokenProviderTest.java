package com.actpro.referral.auth;

import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Phase 9 hardening: JwtTokenProvider previously bound its {@code @Value} fields to the plain
 * {@code jwt.secret}/{@code jwt.expiration} property paths - properties nothing in
 * application.yml ever set, so the JWT_SECRET env var was silently ignored in every environment
 * and every token was signed/verified with the hardcoded fallback literal (found by
 * tenant-security-reviewer's Phase 9 audit). Fixed to bind {@code app.jwt.secret}/
 * {@code app.jwt.expiration-minutes}, matching application.yml/docker-compose.yml. This test
 * exercises the round-trip and cross-secret rejection at the unit level;
 * {@link JwtTokenProviderPropertyBindingTest} is the regression guard that would have caught the
 * original bug (a full Spring context proving the *configured* property actually reaches the bean).
 */
class JwtTokenProviderTest {

    private JwtTokenProvider newProvider(String secret, long expirationMinutes) {
        JwtTokenProvider provider = new JwtTokenProvider();
        ReflectionTestUtils.setField(provider, "jwtSecret", secret);
        ReflectionTestUtils.setField(provider, "jwtExpirationMinutes", expirationMinutes);
        return provider;
    }

    @Test
    void shouldRoundTripAllClaims() {
        JwtTokenProvider provider = newProvider("unit-test-secret-key-at-least-256-bits-long-for-hs256!!", 60);

        String token = provider.generateToken(42L, "amy@example.com", 7L, "AMBASSADOR");

        assertTrue(provider.validateToken(token));
        assertEquals(42L, provider.getUserIdFromToken(token));
        assertEquals("amy@example.com", provider.getUsernameFromToken(token));
        assertEquals(7L, provider.getCompanyIdFromToken(token));
        assertEquals("AMBASSADOR", provider.getRoleFromToken(token));
    }

    @Test
    void shouldRejectATokenSignedWithADifferentSecret() {
        JwtTokenProvider signer = newProvider("secret-one-at-least-256-bits-long-for-hs256-padding!!", 60);
        JwtTokenProvider verifier = newProvider("secret-two-at-least-256-bits-long-for-hs256-padding!!", 60);

        String token = signer.generateToken(1L, "user@example.com", 1L, "COMPANY_ADMIN");

        assertFalse(verifier.validateToken(token));
    }
}
