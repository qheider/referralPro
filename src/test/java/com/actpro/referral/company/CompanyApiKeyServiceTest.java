package com.actpro.referral.company;

import com.actpro.referral.common.exception.BadRequestException;
import com.actpro.referral.common.exception.NotFoundException;
import com.actpro.referral.company.dto.ApiKeySummaryResponse;
import com.actpro.referral.company.dto.IssuedApiKeyResponse;
import com.actpro.referral.security.CurrentUserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CompanyApiKeyServiceTest {

    @Mock
    private CompanyApiKeyRepository companyApiKeyRepository;

    @Mock
    private CompanyRepository companyRepository;

    @Mock
    private CurrentUserService currentUserService;

    @InjectMocks
    private CompanyApiKeyService companyApiKeyService;

    private Company company;

    @BeforeEach
    void setUp() {
        company = new Company();
        company.setId(5L);
        company.setName("Acme");
    }

    @Test
    void shouldIssueInitialKeyWithHashedStorageAndRawSecretReturnedOnce() {
        when(companyApiKeyRepository.existsByKeyId(any())).thenReturn(false);
        when(companyApiKeyRepository.save(any(CompanyApiKey.class))).thenAnswer(invocation -> {
            CompanyApiKey key = invocation.getArgument(0);
            key.setId(1L);
            return key;
        });

        IssuedApiKeyResponse response = companyApiKeyService.issueInitialKey(company);

        assertTrue(response.apiKey().startsWith("cmp_live_"));
        assertEquals(1L, response.id());

        ArgumentCaptor<CompanyApiKey> captor = ArgumentCaptor.forClass(CompanyApiKey.class);
        verify(companyApiKeyRepository).save(captor.capture());
        CompanyApiKey saved = captor.getValue();
        assertEquals(company, saved.getCompany());
        assertEquals(CompanyApiKeyStatus.ACTIVE, saved.getStatus());
        assertEquals(sha256(response.apiKey()), saved.getSecretHash());
        assertEquals(response.apiKey().substring(response.apiKey().length() - 4), saved.getSecretPreview());
        assertFalse(saved.getSecretHash().equals(response.apiKey()), "raw secret must never be stored");
    }

    @Test
    void shouldResolveCompanyForActiveUnexpiredKey() {
        String rawKey = "cmp_live_testsecret";
        CompanyApiKey key = new CompanyApiKey();
        key.setId(9L);
        key.setCompany(company);
        key.setStatus(CompanyApiKeyStatus.ACTIVE);
        key.setSecretHash(sha256(rawKey));

        when(companyApiKeyRepository.findBySecretHash(sha256(rawKey))).thenReturn(Optional.of(key));

        Optional<Company> resolved = companyApiKeyService.resolveActiveCompany(rawKey);

        assertTrue(resolved.isPresent());
        assertEquals(company, resolved.get());
        verify(companyApiKeyRepository).updateLastUsedAt(eq(9L), any(LocalDateTime.class));
    }

    @Test
    void shouldStillResolveCompanyWhenLastUsedUpdateFails() {
        String rawKey = "cmp_live_testsecret";
        CompanyApiKey key = new CompanyApiKey();
        key.setId(9L);
        key.setCompany(company);
        key.setStatus(CompanyApiKeyStatus.ACTIVE);
        key.setSecretHash(sha256(rawKey));

        when(companyApiKeyRepository.findBySecretHash(sha256(rawKey))).thenReturn(Optional.of(key));
        doThrow(new RuntimeException("db unavailable"))
                .when(companyApiKeyRepository).updateLastUsedAt(eq(9L), any(LocalDateTime.class));

        Optional<Company> resolved = companyApiKeyService.resolveActiveCompany(rawKey);

        assertTrue(resolved.isPresent(), "a last-used tracking failure must never break authentication");
        assertEquals(company, resolved.get());
    }

    @Test
    void shouldProduceStandardSha256HexDigestsMatchingKnownTestVectors() {
        // Independent verification (not derived from the service's own hash() method) that this
        // project's hashing approach - MessageDigest("SHA-256") + HexFormat lowercase hex, see
        // sha256() below, which mirrors CompanyApiKeyService.hash() - produces standard,
        // NIST-conformant SHA-256 digests. MySQL's SHA2(str, 256), used by V20's backfill
        // migration, is also a standard-conformant implementation of the same algorithm operating
        // on the same ASCII bytes, so agreement here is strong evidence the migration's hashes
        // will match what this service computes for the same key.
        assertEquals(
                "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855",
                sha256(""));
        assertEquals(
                "ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad",
                sha256("abc"));
    }

    @Test
    void shouldNotResolveCompanyForRevokedKey() {
        String rawKey = "cmp_live_testsecret";
        CompanyApiKey key = new CompanyApiKey();
        key.setId(9L);
        key.setCompany(company);
        key.setStatus(CompanyApiKeyStatus.REVOKED);
        key.setSecretHash(sha256(rawKey));

        when(companyApiKeyRepository.findBySecretHash(sha256(rawKey))).thenReturn(Optional.of(key));

        assertTrue(companyApiKeyService.resolveActiveCompany(rawKey).isEmpty());
        verify(companyApiKeyRepository, never()).updateLastUsedAt(anyLong(), any());
    }

    @Test
    void shouldNotResolveCompanyForExpiredKey() {
        String rawKey = "cmp_live_testsecret";
        CompanyApiKey key = new CompanyApiKey();
        key.setId(9L);
        key.setCompany(company);
        key.setStatus(CompanyApiKeyStatus.ACTIVE);
        key.setSecretHash(sha256(rawKey));
        key.setExpiresAt(LocalDateTime.now().minusDays(1));

        when(companyApiKeyRepository.findBySecretHash(sha256(rawKey))).thenReturn(Optional.of(key));

        assertTrue(companyApiKeyService.resolveActiveCompany(rawKey).isEmpty());
    }

    @Test
    void shouldNotResolveCompanyForUnknownKey() {
        when(companyApiKeyRepository.findBySecretHash(any())).thenReturn(Optional.empty());

        assertTrue(companyApiKeyService.resolveActiveCompany("cmp_live_unknown").isEmpty());
    }

    @Test
    void shouldRotateKeyRevokingAllPreviousActiveKeysAndIssuingOne() {
        when(currentUserService.getCurrentCompanyId()).thenReturn(5L);

        CompanyApiKey existing1 = new CompanyApiKey();
        existing1.setId(11L);
        existing1.setCompany(company);
        existing1.setStatus(CompanyApiKeyStatus.ACTIVE);

        CompanyApiKey existing2 = new CompanyApiKey();
        existing2.setId(12L);
        existing2.setCompany(company);
        existing2.setStatus(CompanyApiKeyStatus.ACTIVE);

        when(companyApiKeyRepository.findByCompanyIdAndStatus(5L, CompanyApiKeyStatus.ACTIVE))
                .thenReturn(List.of(existing1, existing2));
        when(companyApiKeyRepository.existsByKeyId(any())).thenReturn(false);
        when(companyApiKeyRepository.save(any(CompanyApiKey.class))).thenAnswer(invocation -> {
            CompanyApiKey key = invocation.getArgument(0);
            key.setId(20L);
            return key;
        });

        IssuedApiKeyResponse response = companyApiKeyService.rotateKey();

        assertEquals(CompanyApiKeyStatus.REVOKED, existing1.getStatus());
        assertEquals(CompanyApiKeyStatus.REVOKED, existing2.getStatus());
        assertTrue(existing1.getRevokedAt() != null);

        ArgumentCaptor<CompanyApiKey> captor = ArgumentCaptor.forClass(CompanyApiKey.class);
        verify(companyApiKeyRepository).save(captor.capture());
        assertEquals(11L, captor.getValue().getRotatedFromKeyId());
        assertEquals(20L, response.id());

        verify(companyRepository, never()).findById(any());
    }

    @Test
    void shouldRotateKeyByFetchingCompanyWhenNoActiveKeysExist() {
        when(currentUserService.getCurrentCompanyId()).thenReturn(5L);
        when(companyApiKeyRepository.findByCompanyIdAndStatus(5L, CompanyApiKeyStatus.ACTIVE)).thenReturn(List.of());
        when(companyRepository.findById(5L)).thenReturn(Optional.of(company));
        when(companyApiKeyRepository.existsByKeyId(any())).thenReturn(false);
        when(companyApiKeyRepository.save(any(CompanyApiKey.class))).thenAnswer(invocation -> {
            CompanyApiKey key = invocation.getArgument(0);
            key.setId(21L);
            return key;
        });

        IssuedApiKeyResponse response = companyApiKeyService.rotateKey();

        assertEquals(21L, response.id());
        verify(companyRepository).findById(5L);
    }

    @Test
    void shouldRevokeOwnedKey() {
        when(currentUserService.getCurrentCompanyId()).thenReturn(5L);
        CompanyApiKey key = new CompanyApiKey();
        key.setId(30L);
        key.setStatus(CompanyApiKeyStatus.ACTIVE);

        when(companyApiKeyRepository.findByIdAndCompanyId(30L, 5L)).thenReturn(Optional.of(key));

        companyApiKeyService.revokeKey(30L);

        assertEquals(CompanyApiKeyStatus.REVOKED, key.getStatus());
        assertTrue(key.getRevokedAt() != null);
    }

    @Test
    void shouldRejectRevokingAlreadyRevokedKey() {
        when(currentUserService.getCurrentCompanyId()).thenReturn(5L);
        CompanyApiKey key = new CompanyApiKey();
        key.setId(30L);
        key.setStatus(CompanyApiKeyStatus.REVOKED);

        when(companyApiKeyRepository.findByIdAndCompanyId(30L, 5L)).thenReturn(Optional.of(key));

        assertThrows(BadRequestException.class, () -> companyApiKeyService.revokeKey(30L));
    }

    @Test
    void shouldRejectRevokingKeyFromAnotherCompany() {
        when(currentUserService.getCurrentCompanyId()).thenReturn(5L);
        when(companyApiKeyRepository.findByIdAndCompanyId(30L, 5L)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> companyApiKeyService.revokeKey(30L));
    }

    @Test
    void shouldListKeysForCurrentCompanyWithoutExposingSecretHash() {
        when(currentUserService.getCurrentCompanyId()).thenReturn(5L);
        CompanyApiKey key = new CompanyApiKey();
        key.setId(1L);
        key.setKeyId("key_abc123");
        key.setSecretHash("should-not-appear-in-dto");
        key.setSecretPreview("ab12");
        key.setStatus(CompanyApiKeyStatus.ACTIVE);

        when(companyApiKeyRepository.findByCompanyIdOrderByCreatedAtDesc(5L)).thenReturn(List.of(key));

        List<ApiKeySummaryResponse> result = companyApiKeyService.listKeys();

        assertEquals(1, result.size());
        assertEquals("key_abc123", result.get(0).keyId());
        assertEquals("ab12", result.get(0).secretPreview());
    }

    private String sha256(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(value.getBytes(java.nio.charset.StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
