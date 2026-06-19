package com.actpro.referral.user;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PlatformUserRepository extends JpaRepository<PlatformUser, Long> {

    Optional<PlatformUser> findByCompanyIdAndExternalUserId(Long companyId, String externalUserId);
}
