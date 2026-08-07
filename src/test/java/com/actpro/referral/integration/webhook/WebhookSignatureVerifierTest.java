package com.actpro.referral.integration.webhook;

import org.junit.jupiter.api.Test;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.HexFormat;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WebhookSignatureVerifierTest {

    private static final String SECRET = "test-webhook-secret";
    private static final int TOLERANCE_SECONDS = 300;

    private final WebhookSignatureVerifier verifier = new WebhookSignatureVerifier();

    @Test
    void shouldAcceptValidSignature() {
        String body = "{\"eventId\":\"evt_1\"}";
        String timestamp = String.valueOf(Instant.now().getEpochSecond());
        String signature = sign(timestamp, body, SECRET);

        assertTrue(verifier.isValid(body, signature, timestamp, SECRET, TOLERANCE_SECONDS));
    }

    @Test
    void shouldRejectTamperedBody() {
        String body = "{\"eventId\":\"evt_1\"}";
        String timestamp = String.valueOf(Instant.now().getEpochSecond());
        String signature = sign(timestamp, body, SECRET);

        assertFalse(verifier.isValid("{\"eventId\":\"evt_2\"}", signature, timestamp, SECRET, TOLERANCE_SECONDS));
    }

    @Test
    void shouldRejectTamperedSignature() {
        String body = "{\"eventId\":\"evt_1\"}";
        String timestamp = String.valueOf(Instant.now().getEpochSecond());
        String signature = sign(timestamp, body, SECRET);
        String tampered = signature.substring(0, signature.length() - 2) + (signature.endsWith("00") ? "11" : "00");

        assertFalse(verifier.isValid(body, tampered, timestamp, SECRET, TOLERANCE_SECONDS));
    }

    @Test
    void shouldRejectWrongSecret() {
        String body = "{\"eventId\":\"evt_1\"}";
        String timestamp = String.valueOf(Instant.now().getEpochSecond());
        String signature = sign(timestamp, body, SECRET);

        assertFalse(verifier.isValid(body, signature, timestamp, "a-different-secret", TOLERANCE_SECONDS));
    }

    @Test
    void shouldRejectStaleTimestamp() {
        String body = "{\"eventId\":\"evt_1\"}";
        String staleTimestamp = String.valueOf(Instant.now().getEpochSecond() - 600);
        String signature = sign(staleTimestamp, body, SECRET);

        assertFalse(verifier.isValid(body, signature, staleTimestamp, SECRET, TOLERANCE_SECONDS));
    }

    @Test
    void shouldRejectFutureTimestampBeyondTolerance() {
        String body = "{\"eventId\":\"evt_1\"}";
        String futureTimestamp = String.valueOf(Instant.now().getEpochSecond() + 600);
        String signature = sign(futureTimestamp, body, SECRET);

        assertFalse(verifier.isValid(body, signature, futureTimestamp, SECRET, TOLERANCE_SECONDS));
    }

    @Test
    void shouldRejectMalformedHexSignature() {
        String body = "{\"eventId\":\"evt_1\"}";
        String timestamp = String.valueOf(Instant.now().getEpochSecond());

        assertFalse(verifier.isValid(body, "not-valid-hex!!", timestamp, SECRET, TOLERANCE_SECONDS));
    }

    @Test
    void shouldRejectMissingHeaders() {
        assertFalse(verifier.isValid("{}", null, "123", SECRET, TOLERANCE_SECONDS));
        assertFalse(verifier.isValid("{}", "abc", null, SECRET, TOLERANCE_SECONDS));
    }

    @Test
    void shouldRejectNonNumericTimestamp() {
        String body = "{}";
        assertFalse(verifier.isValid(body, "abc123", "not-a-number", SECRET, TOLERANCE_SECONDS));
    }

    private String sign(String timestamp, String body, String secret) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] digest = mac.doFinal((timestamp + "." + body).getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }
}
