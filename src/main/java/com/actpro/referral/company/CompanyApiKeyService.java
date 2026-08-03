package com.actpro.referral.company;

import com.actpro.referral.common.exception.BadRequestException;
import com.actpro.referral.common.exception.NotFoundException;
import com.actpro.referral.company.dto.ApiKeySummaryResponse;
import com.actpro.referral.company.dto.IssuedApiKeyResponse;
import com.actpro.referral.security.CurrentUserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class CompanyApiKeyService {

    private static final String SECRET_ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz23456789";
    private static final String KEY_ID_ALPHABET = "abcdefghijklmnopqrstuvwxyz0123456789";
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final CompanyApiKeyRepository companyApiKeyRepository;
    private final CompanyRepository companyRepository;
    private final CurrentUserService currentUserService;

    /**
     * Resolves an authenticated Company from a raw API key presented on the wire. Returns empty
     * if the hash matches no key, or matches one that is not currently usable (revoked, expired).
     * Not read-only: successful resolution best-effort records last-used-at. Status transitions
     * on expiry are a worker's job, not something to write on every request.
     */
    @Transactional
    public Optional<Company> resolveActiveCompany(String rawKey) {
        return companyApiKeyRepository.findBySecretHash(hash(rawKey))
                .filter(key -> key.getStatus() == CompanyApiKeyStatus.ACTIVE)
                .filter(key -> key.getExpiresAt() == null || key.getExpiresAt().isAfter(LocalDateTime.now()))
                .map(key -> {
                    recordUsageBestEffort(key.getId());
                    return key.getCompany();
                });
    }

    @Transactional
    public IssuedApiKeyResponse issueInitialKey(Company company) {
        return persistNewKey(company, null);
    }

    @Transactional
    public IssuedApiKeyResponse rotateKey() {
        Long companyId = currentUserService.getCurrentCompanyId();
        List<CompanyApiKey> activeKeys = companyApiKeyRepository.findByCompanyIdAndStatus(companyId, CompanyApiKeyStatus.ACTIVE);

        Company company = activeKeys.stream()
                .findFirst()
                .map(CompanyApiKey::getCompany)
                .orElseGet(() -> companyRepository.findById(companyId)
                        .orElseThrow(() -> new NotFoundException("Company not found")));

        // In practice a company has at most one active key today (issueInitialKey/rotateKey never
        // leave more than one ACTIVE), so recording lineage from the first is a 1:1 link in
        // practice; if that ever changes, rotatedFromKeyId would need to become a collection.
        LocalDateTime now = LocalDateTime.now();
        Long previousKeyId = activeKeys.isEmpty() ? null : activeKeys.get(0).getId();
        for (CompanyApiKey key : activeKeys) {
            key.setStatus(CompanyApiKeyStatus.REVOKED);
            key.setRevokedAt(now);
        }

        return persistNewKey(company, previousKeyId);
    }

    @Transactional
    public void revokeKey(Long keyRecordId) {
        Long companyId = currentUserService.getCurrentCompanyId();
        CompanyApiKey key = companyApiKeyRepository.findByIdAndCompanyId(keyRecordId, companyId)
                .orElseThrow(() -> new NotFoundException("API key not found"));

        if (key.getStatus() == CompanyApiKeyStatus.REVOKED) {
            throw new BadRequestException("API key is already revoked");
        }

        key.setStatus(CompanyApiKeyStatus.REVOKED);
        key.setRevokedAt(LocalDateTime.now());
    }

    @Transactional(readOnly = true)
    public List<ApiKeySummaryResponse> listKeys() {
        Long companyId = currentUserService.getCurrentCompanyId();
        return companyApiKeyRepository.findByCompanyIdOrderByCreatedAtDesc(companyId).stream()
                .map(this::toSummary)
                .toList();
    }

    private void recordUsageBestEffort(Long keyRecordId) {
        try {
            companyApiKeyRepository.updateLastUsedAt(keyRecordId, LocalDateTime.now());
        } catch (Exception ex) {
            log.warn("Failed to record API key last-used timestamp for key {}: {}", keyRecordId, ex.getMessage());
        }
    }

    private IssuedApiKeyResponse persistNewKey(Company company, Long rotatedFromKeyId) {
        String rawSecret = "cmp_live_" + randomString(SECRET_ALPHABET, 32);

        CompanyApiKey key = new CompanyApiKey();
        key.setCompany(company);
        key.setKeyId(generateUniqueKeyId());
        key.setSecretHash(hash(rawSecret));
        key.setSecretPreview(rawSecret.substring(rawSecret.length() - 4));
        key.setStatus(CompanyApiKeyStatus.ACTIVE);
        key.setRotatedFromKeyId(rotatedFromKeyId);
        key = companyApiKeyRepository.save(key);

        return new IssuedApiKeyResponse(key.getId(), key.getKeyId(), rawSecret, key.getCreatedAt());
    }

    private String generateUniqueKeyId() {
        String keyId;
        do {
            keyId = "key_" + randomString(KEY_ID_ALPHABET, 12);
        } while (companyApiKeyRepository.existsByKeyId(keyId));
        return keyId;
    }

    private ApiKeySummaryResponse toSummary(CompanyApiKey key) {
        return new ApiKeySummaryResponse(
                key.getId(),
                key.getKeyId(),
                key.getSecretPreview(),
                key.getStatus(),
                key.getCreatedAt(),
                key.getExpiresAt(),
                key.getLastUsedAt(),
                key.getRevokedAt()
        );
    }

    private String hash(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashed = digest.digest(value.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hashed);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }

    private String randomString(String alphabet, int length) {
        StringBuilder builder = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            builder.append(alphabet.charAt(SECURE_RANDOM.nextInt(alphabet.length())));
        }
        return builder.toString();
    }
}
