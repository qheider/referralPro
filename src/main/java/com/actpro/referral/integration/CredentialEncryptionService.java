package com.actpro.referral.integration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Base64;

/**
 * Symmetric (reversible) encryption for outgoing-integration secrets - company API credentials
 * and, later, the webhook signing secret - which must be decryptable to be sent back out on a
 * live HTTP call, unlike {@link com.actpro.referral.company.CompanyApiKeyService}'s one-way
 * SHA-256 hashing of ReferralPro's own generated API keys.
 * <p>
 * AES-256-GCM with a random 12-byte IV per call; the IV is prepended to the ciphertext and the
 * pair is Base64-encoded as a single opaque string. The key is derived by SHA-256-hashing the
 * configured secret string down to 32 bytes, so any-length passphrase can be supplied via env var
 * - same insecure-default-with-required-production-override pattern as
 * {@code JwtTokenProvider.jwtSecret}. Decrypt is only ever called from
 * {@link ApiSubmissionDispatchService} and {@link CompanyIntegrationService#testConnection()} -
 * never from a controller response path - so raw credentials structurally cannot leak through the
 * config API.
 */
@Service
public class CredentialEncryptionService {

    private static final String TRANSFORMATION = "AES/GCM/NoPadding";
    private static final int GCM_IV_LENGTH_BYTES = 12;
    private static final int GCM_TAG_LENGTH_BITS = 128;

    private final SecretKeySpec key;
    private final SecureRandom secureRandom = new SecureRandom();

    public CredentialEncryptionService(
            @Value("${app.integration.encryption-key:dev-only-insecure-integration-key-change-me}") String encryptionKey) {
        this.key = new SecretKeySpec(deriveKeyBytes(encryptionKey), "AES");
    }

    public String encrypt(String plaintext) {
        if (plaintext == null) {
            return null;
        }
        try {
            byte[] iv = new byte[GCM_IV_LENGTH_BYTES];
            secureRandom.nextBytes(iv);

            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.ENCRYPT_MODE, key, new GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv));
            byte[] ciphertext = cipher.doFinal(plaintext.getBytes(StandardCharsets.UTF_8));

            byte[] combined = new byte[iv.length + ciphertext.length];
            System.arraycopy(iv, 0, combined, 0, iv.length);
            System.arraycopy(ciphertext, 0, combined, iv.length, ciphertext.length);
            return Base64.getEncoder().encodeToString(combined);
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("Failed to encrypt integration credential", e);
        }
    }

    public String decrypt(String encoded) {
        if (encoded == null) {
            return null;
        }
        try {
            byte[] combined = Base64.getDecoder().decode(encoded);
            if (combined.length < GCM_IV_LENGTH_BYTES) {
                throw new IllegalArgumentException("Encrypted credential payload is too short");
            }
            byte[] iv = Arrays.copyOfRange(combined, 0, GCM_IV_LENGTH_BYTES);
            byte[] ciphertext = Arrays.copyOfRange(combined, GCM_IV_LENGTH_BYTES, combined.length);

            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.DECRYPT_MODE, key, new GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv));
            byte[] plaintext = cipher.doFinal(ciphertext);
            return new String(plaintext, StandardCharsets.UTF_8);
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("Failed to decrypt integration credential - wrong key or tampered data", e);
        }
    }

    private static byte[] deriveKeyBytes(String encryptionKey) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return digest.digest(encryptionKey.getBytes(StandardCharsets.UTF_8));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }
}
