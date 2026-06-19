package com.actpro.referral.user;

import com.actpro.referral.company.Company;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class PlatformUserService {

    private final PlatformUserRepository platformUserRepository;

    @Transactional
    public PlatformUser findOrCreate(Company company, String externalUserId, String email, String name) {
        log.debug("Finding or creating platform user: companyId={}, externalUserId={}", company.getId(), externalUserId);

        return platformUserRepository
                .findByCompanyIdAndExternalUserId(company.getId(), externalUserId)
                .orElseGet(() -> {
                    log.info("Creating new platform user: companyId={}, externalUserId={}", company.getId(), externalUserId);
                    PlatformUser user = new PlatformUser();
                    user.setCompany(company);
                    user.setExternalUserId(externalUserId);
                    user.setEmail(email);
                    user.setName(name);
                    return platformUserRepository.save(user);
                });
    }
}
