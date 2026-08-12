package com.actpro.referral.integration.webhook;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.HexFormat;

/**
 * Verifies an inbound company webhook's HMAC-SHA256 signature and timestamp freshness. Pure and
 * stateless - no Spring/DB dependency beyond being a bean for injection - so it's directly
 * unit-testable.
 * <p>
 * Signed content is {@code timestamp + "." + rawBody} (Stripe-style): binding the timestamp into
 * the signature means a captured valid request can't be replayed later with a substituted fresh
 * timestamp. Both the signature binding and the freshness check use the caller-supplied
 * {@code timestampHeader} value exclusively - never a timestamp derived from the (unauthenticated
 * until verified) JSON body - so a security decision is never influenced by unverified content.
 */
@Component
@Slf4j
public class WebhookSignatureVerifier {

    private static final String HMAC_ALGORITHM = "HmacSHA256";

    public boolean isValid(String rawBody, String signatureHeader, String timestampHeader, String secret, int toleranceSeconds) {
        if (rawBody == null || signatureHeader == null || timestampHeader == null || secret == null) {
            return false;
        }

        long timestamp;
        try {
            timestamp = Long.parseLong(timestampHeader.trim());
        } catch (NumberFormatException e) {
            return false;
        }

        long now = Instant.now().getEpochSecond();
        if (Math.abs(now - timestamp) > toleranceSeconds) {
            return false;
        }

        byte[] providedSignature;
        try {
            providedSignature = HexFormat.of().parseHex(signatureHeader.trim());
        } catch (IllegalArgumentException e) {
            return false;
        }

        byte[] expectedSignature;
        try {
            expectedSignature = hmacSha256(timestampHeader.trim() + "." + rawBody, secret);
        } catch (GeneralSecurityException e) {
            log.warn("Failed to compute expected webhook signature: {}", e.getMessage());
            return false;
        }

        return MessageDigest.isEqual(expectedSignature, providedSignature);
    }

    private byte[] hmacSha256(String content, String secret) throws GeneralSecurityException {
        Mac mac = Mac.getInstance(HMAC_ALGORITHM);
        mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), HMAC_ALGORITHM));
        return mac.doFinal(content.getBytes(StandardCharsets.UTF_8));
    }
}
