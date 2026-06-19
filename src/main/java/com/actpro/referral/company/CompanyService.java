package com.actpro.referral.company;

import com.actpro.referral.common.exception.BadRequestException;
import com.actpro.referral.company.dto.RegisterCompanyRequest;
import com.actpro.referral.company.dto.RegisterCompanyResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class CompanyService {

    private final CompanyRepository companyRepository;

    @Transactional
    public RegisterCompanyResponse registerCompany(RegisterCompanyRequest request) {
        log.info("Registering company with email: {}", request.email());

        // Check if email already exists
        if (companyRepository.existsByEmail(request.email())) {
            throw new BadRequestException("Company with this email already exists");
        }

        // Create company
        Company company = new Company();
        company.setName(request.name());
        company.setEmail(request.email());
        company.setStatus(CompanyStatus.ACTIVE);
        company.setApiKey(generateApiKey());

        company = companyRepository.save(company);
        log.info("Company registered successfully with ID: {}", company.getId());

        return new RegisterCompanyResponse(
                company.getId(),
                company.getName(),
                company.getApiKey()
        );
    }

    private String generateApiKey() {
        String uuid = UUID.randomUUID().toString().replace("-", "");
        return "cmp_live_" + uuid;
    }
}
