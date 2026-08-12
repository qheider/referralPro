package com.actpro.referral.integration;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class CredentialEncryptionServiceTest {

    private CredentialEncryptionService encryptionService;

    @BeforeEach
    void setUp() {
        encryptionService = new CredentialEncryptionService("test-encryption-key-for-unit-tests");
    }

    @Test
    void shouldRoundTripEncryptAndDecrypt() {
        String plaintext = "{\"apiKey\":\"super-secret-value\"}";

        String encrypted = encryptionService.encrypt(plaintext);
        String decrypted = encryptionService.decrypt(encrypted);

        assertEquals(plaintext, decrypted);
    }

    @Test
    void shouldProduceDifferentCiphertextForSamePlaintextDueToRandomIv() {
        String plaintext = "{\"token\":\"same-value\"}";

        String first = encryptionService.encrypt(plaintext);
        String second = encryptionService.encrypt(plaintext);

        assertNotEquals(first, second);
        // Both still decrypt to the same plaintext.
        assertEquals(plaintext, encryptionService.decrypt(first));
        assertEquals(plaintext, encryptionService.decrypt(second));
    }

    @Test
    void shouldRejectTamperedCiphertext() {
        String encrypted = encryptionService.encrypt("{\"token\":\"value\"}");
        // Flip a character in the middle of the Base64 payload to corrupt the GCM auth tag/ciphertext.
        char[] chars = encrypted.toCharArray();
        int mid = chars.length / 2;
        chars[mid] = chars[mid] == 'A' ? 'B' : 'A';
        String tampered = new String(chars);

        assertThrows(IllegalStateException.class, () -> encryptionService.decrypt(tampered));
    }

    @Test
    void shouldReturnNullForNullInput() {
        assertNull(encryptionService.encrypt(null));
        assertNull(encryptionService.decrypt(null));
    }

    @Test
    void shouldFailToDecryptWithDifferentKey() {
        String encrypted = encryptionService.encrypt("{\"token\":\"value\"}");
        CredentialEncryptionService otherKeyService = new CredentialEncryptionService("a-completely-different-key");

        assertThrows(IllegalStateException.class, () -> otherKeyService.decrypt(encrypted));
    }
}
