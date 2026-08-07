package com.actpro.referral.auth;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Regression guard for the Phase 9 finding: boots the real Spring context so property binding is
 * exercised end-to-end exactly as in production, rather than constructing {@link JwtTokenProvider}
 * by hand. Proves the bean actually signs with the value configured at
 * {@code app.jwt.secret} (application-test.yml: "test-secret-key-for-testing-purposes-only-256-bits"),
 * not the class's hardcoded fallback literal - the original bug would have signed with the
 * fallback regardless of what was configured, so this test would have failed against the buggy
 * property path.
 */
@SpringBootTest
@ActiveProfiles("test")
class JwtTokenProviderPropertyBindingTest {

    private static final String CONFIGURED_TEST_SECRET = "test-secret-key-for-testing-purposes-only-256-bits";
    private static final String HARDCODED_FALLBACK_SECRET = "your-super-secret-jwt-key-that-is-at-least-256-bits-long-for-hs256";

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @Test
    void shouldSignWithTheConfiguredAppJwtSecretNotTheHardcodedFallback() {
        String token = jwtTokenProvider.generateToken(1L, "user@example.com", 1L, "COMPANY_ADMIN");

        assertEquals(1L, verifyAndExtractUserId(token, CONFIGURED_TEST_SECRET), "Token must verify against the configured app.jwt.secret");
        assertThrows(Exception.class, () -> verifyAndExtractUserId(token, HARDCODED_FALLBACK_SECRET),
                "Token must NOT verify against the hardcoded fallback secret once a real secret is configured");
    }

    private Long verifyAndExtractUserId(String token, String secret) {
        SecretKey key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        String subject = Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload().getSubject();
        return Long.parseLong(subject);
    }
}
