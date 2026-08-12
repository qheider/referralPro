package com.actpro.referral.company;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface CompanyApiKeyRepository extends JpaRepository<CompanyApiKey, Long> {

    Optional<CompanyApiKey> findBySecretHash(String secretHash);

    Optional<CompanyApiKey> findByIdAndCompanyId(Long id, Long companyId);

    List<CompanyApiKey> findByCompanyIdOrderByCreatedAtDesc(Long companyId);

    List<CompanyApiKey> findByCompanyIdAndStatus(Long companyId, CompanyApiKeyStatus status);

    boolean existsByKeyId(String keyId);

    @Modifying(clearAutomatically = true)
    @Query("UPDATE CompanyApiKey k SET k.lastUsedAt = :usedAt WHERE k.id = :id")
    void updateLastUsedAt(@Param("id") Long id, @Param("usedAt") LocalDateTime usedAt);
}
