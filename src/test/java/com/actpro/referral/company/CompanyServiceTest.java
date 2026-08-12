package com.actpro.referral.company;

import com.actpro.referral.auth.AccountInvitationService;
import com.actpro.referral.auth.DashboardUser;
import com.actpro.referral.auth.DashboardUserRepository;
import com.actpro.referral.auth.InvitationPurpose;
import com.actpro.referral.auth.UserStatus;
import com.actpro.referral.auth.dto.IssuedInvitationResponse;
import com.actpro.referral.common.exception.BadRequestException;
import com.actpro.referral.company.dto.IssuedApiKeyResponse;
import com.actpro.referral.company.dto.RegisterCompanyRequest;
import com.actpro.referral.company.dto.RegisterCompanyResponse;
import com.actpro.referral.integration.webhook.WebhookPublicIdGenerator;
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
import static org.mockito.ArgumentMatchers.eq;
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

    @Mock
    private CompanyIntegrationRepository companyIntegrationRepository;

    @Mock
    private AccountInvitationService accountInvitationService;

    @Mock
    private WebhookPublicIdGenerator webhookPublicIdGenerator;

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
        when(webhookPublicIdGenerator.generateUniqueId()).thenReturn("WEBHOOKPUBLICID1");
        when(passwordEncoder.encode(request.password())).thenReturn("encoded-password");
        when(dashboardUserRepository.save(any(DashboardUser.class))).thenAnswer(invocation -> {
            DashboardUser user = invocation.getArgument(0);
            user.setId(42L);
            return user;
        });
        LocalDateTime verificationExpiry = LocalDateTime.now().plusDays(7);
        when(accountInvitationService.issueInvitation(any(DashboardUser.class), eq(InvitationPurpose.COMPANY_EMAIL_VERIFICATION)))
                .thenReturn(new IssuedInvitationResponse(1L, "raw-verification-token", verificationExpiry));

        RegisterCompanyResponse response = companyService.registerCompany(request);

        assertEquals(7L, response.companyId());
        assertEquals("cmp_live_rawsecret", response.apiKey());
        assertEquals("raw-verification-token", response.emailVerificationToken());
        assertEquals(verificationExpiry, response.emailVerificationTokenExpiresAt());

        ArgumentCaptor<Company> companyCaptor = ArgumentCaptor.forClass(Company.class);
        verify(companyApiKeyService).issueInitialKey(companyCaptor.capture());
        assertEquals(7L, companyCaptor.getValue().getId());

        ArgumentCaptor<CompanyIntegration> integrationCaptor = ArgumentCaptor.forClass(CompanyIntegration.class);
        verify(companyIntegrationRepository).save(integrationCaptor.capture());
        assertEquals(CompanyIntegrationStatus.NOT_CONFIGURED, integrationCaptor.getValue().getStatus());
        assertEquals(7L, integrationCaptor.getValue().getCompany().getId());
        assertEquals("WEBHOOKPUBLICID1", integrationCaptor.getValue().getWebhookPublicId());

        ArgumentCaptor<DashboardUser> userCaptor = ArgumentCaptor.forClass(DashboardUser.class);
        verify(dashboardUserRepository).save(userCaptor.capture());
        assertEquals("encoded-password", userCaptor.getValue().getPassword());
        assertEquals(UserStatus.PENDING, userCaptor.getValue().getStatus());
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
