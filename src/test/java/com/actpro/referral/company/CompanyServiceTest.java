package com.actpro.referral.company;

import com.actpro.referral.auth.DashboardUser;
import com.actpro.referral.auth.DashboardUserRepository;
import com.actpro.referral.common.exception.BadRequestException;
import com.actpro.referral.company.dto.IssuedApiKeyResponse;
import com.actpro.referral.company.dto.RegisterCompanyRequest;
import com.actpro.referral.company.dto.RegisterCompanyResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CompanyServiceTest {

    @Mock
    private CompanyRepository companyRepository;

    @Mock
    private DashboardUserRepository dashboardUserRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private CompanyApiKeyService companyApiKeyService;

    @InjectMocks
    private CompanyService companyService;

    @Test
    void shouldRegisterCompanyAndReturnRawApiKeyFromIssuedKey() {
        RegisterCompanyRequest request = validRequest();

        when(companyRepository.existsByEmail(request.companyEmail())).thenReturn(false);
        when(dashboardUserRepository.existsByUsername(request.adminWorkEmail())).thenReturn(false);
        when(companyRepository.save(any(Company.class))).thenAnswer(invocation -> {
            Company company = invocation.getArgument(0);
            company.setId(7L);
            return company;
        });
        when(companyApiKeyService.issueInitialKey(any(Company.class)))
                .thenReturn(new IssuedApiKeyResponse(1L, "key_abc123", "cmp_live_rawsecret", LocalDateTime.now()));
        when(passwordEncoder.encode(request.password())).thenReturn("encoded-password");

        RegisterCompanyResponse response = companyService.registerCompany(request);

        assertEquals(7L, response.getCompanyId());
        assertEquals("cmp_live_rawsecret", response.getApiKey());

        ArgumentCaptor<Company> companyCaptor = ArgumentCaptor.forClass(Company.class);
        verify(companyApiKeyService).issueInitialKey(companyCaptor.capture());
        assertEquals(7L, companyCaptor.getValue().getId());

        ArgumentCaptor<DashboardUser> userCaptor = ArgumentCaptor.forClass(DashboardUser.class);
        verify(dashboardUserRepository).save(userCaptor.capture());
        assertEquals("encoded-password", userCaptor.getValue().getPassword());
    }

    @Test
    void shouldRejectRegistrationWithoutAcceptedTerms() {
        RegisterCompanyRequest request = requestWithTerms(false);

        assertThrows(BadRequestException.class, () -> companyService.registerCompany(request));
        verify(companyApiKeyService, never()).issueInitialKey(any());
    }

    @Test
    void shouldRejectRegistrationWhenCompanyEmailAlreadyExists() {
        RegisterCompanyRequest request = validRequest();
        when(companyRepository.existsByEmail(request.companyEmail())).thenReturn(true);

        assertThrows(BadRequestException.class, () -> companyService.registerCompany(request));
        verify(companyApiKeyService, never()).issueInitialKey(any());
    }

    private RegisterCompanyRequest validRequest() {
        return requestWithTerms(true);
    }

    private RegisterCompanyRequest requestWithTerms(boolean acceptedTerms) {
        return new RegisterCompanyRequest(
                "Acme",
                "acme@example.com",
                "https://acme.example.com",
                "Technology",
                "United States",
                null,
                "Jane Admin",
                "jane@acme.example.com",
                "555-0100",
                "password123",
                "COMPANY_ADMIN",
                "11-50",
                "USD",
                acceptedTerms,
                null, null, null, null, null,
                null, null, null,
                null, null, null, null, null, null, null
        );
    }
}
