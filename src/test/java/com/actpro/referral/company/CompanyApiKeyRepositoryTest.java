package com.actpro.referral.company;

import com.actpro.referral.config.JpaConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test")
@Import(JpaConfig.class)
class CompanyApiKeyRepositoryTest {

    @Autowired
    private CompanyApiKeyRepository companyApiKeyRepository;

    @Autowired
    private CompanyRepository companyRepository;

    private Long companyId;
    private Long otherCompanyId;
    private Long keyRecordId;

    @BeforeEach
    void setUp() {
        Company company = new Company();
        company.setName("Acme");
        company.setEmail("acme@example.com");
        company.setStatus(CompanyStatus.ACTIVE);
        company = companyRepository.save(company);
        companyId = company.getId();

        Company otherCompany = new Company();
        otherCompany.setName("Globex");
        otherCompany.setEmail("globex@example.com");
        otherCompany.setStatus(CompanyStatus.ACTIVE);
        otherCompanyId = companyRepository.save(otherCompany).getId();

        CompanyApiKey key = new CompanyApiKey();
        key.setCompany(company);
        key.setKeyId("key_abc123");
        key.setSecretHash("deadbeef");
        key.setSecretPreview("beef");
        key.setStatus(CompanyApiKeyStatus.ACTIVE);
        keyRecordId = companyApiKeyRepository.save(key).getId();
    }

    @Test
    void shouldFindBySecretHash() {
        Optional<CompanyApiKey> found = companyApiKeyRepository.findBySecretHash("deadbeef");

        assertTrue(found.isPresent());
        assertEquals("key_abc123", found.get().getKeyId());
    }

    @Test
    void shouldReturnEmptyForUnknownHash() {
        assertTrue(companyApiKeyRepository.findBySecretHash("not-a-real-hash").isEmpty());
    }

    @Test
    void shouldDetectExistingKeyId() {
        assertTrue(companyApiKeyRepository.existsByKeyId("key_abc123"));
        assertTrue(!companyApiKeyRepository.existsByKeyId("key_does_not_exist"));
    }

    @Test
    void shouldUpdateLastUsedAtAndBeVisibleAfterClear() {
        LocalDateTime usedAt = LocalDateTime.now().withNano(0);

        companyApiKeyRepository.updateLastUsedAt(keyRecordId, usedAt);

        CompanyApiKey reloaded = companyApiKeyRepository.findById(keyRecordId).orElseThrow();
        assertEquals(usedAt, reloaded.getLastUsedAt());
    }

    @Test
    void shouldFindKeyOnlyWhenCompanyIdMatches() {
        assertTrue(companyApiKeyRepository.findByIdAndCompanyId(keyRecordId, companyId).isPresent());
    }

    @Test
    void shouldNotFindKeyOwnedByAnotherCompany() {
        assertTrue(companyApiKeyRepository.findByIdAndCompanyId(keyRecordId, otherCompanyId).isEmpty());
    }
}
