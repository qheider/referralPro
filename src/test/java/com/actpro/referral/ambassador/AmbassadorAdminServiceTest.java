package com.actpro.referral.ambassador;

import com.actpro.referral.ambassador.dto.AmbassadorDetailResponse;
import com.actpro.referral.ambassador.dto.AmbassadorSummaryResponse;
import com.actpro.referral.ambassador.dto.CreateAmbassadorRequest;
import com.actpro.referral.auth.DashboardUser;
import com.actpro.referral.auth.DashboardUserRepository;
import com.actpro.referral.auth.UserRole;
import com.actpro.referral.auth.UserStatus;
import com.actpro.referral.company.Company;
import com.actpro.referral.company.CompanyRepository;
import com.actpro.referral.company.CompanyStatus;
import com.actpro.referral.referral.ReferralLink;
import com.actpro.referral.referral.ReferralLinkRepository;
import com.actpro.referral.referral.ReferralLinkStatus;
import com.actpro.referral.referral.ReferralRepository;
import com.actpro.referral.security.CurrentUserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AmbassadorAdminServiceTest {

    @Mock
    private AmbassadorProfileRepository ambassadorProfileRepository;

    @Mock
    private CampaignAmbassadorAssignmentRepository assignmentRepository;

    @Mock
    private DashboardUserRepository dashboardUserRepository;

    @Mock
    private CompanyRepository companyRepository;

    @Mock
    private ReferralRepository referralRepository;

    @Mock
    private ReferralLinkRepository referralLinkRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private CurrentUserService currentUserService;

    @InjectMocks
    private AmbassadorAdminService ambassadorAdminService;

    private Company company;

    @BeforeEach
    void setUp() {
        company = new Company();
        company.setId(10L);
        company.setName("Acme Rentals");
        company.setStatus(CompanyStatus.ACTIVE);

        when(currentUserService.getCurrentCompanyId()).thenReturn(10L);
        when(assignmentRepository.countByAmbassadorUserIdAndCompanyIdAndStatus(any(), any(), any())).thenReturn(0L);
        when(referralRepository.countByAmbassadorUserIdAndCompanyIdAndStatusIn(any(), any(), any())).thenReturn(0L);
        when(referralRepository.countByAmbassadorUserIdAndCompanyIdAndStatus(any(), any(), any())).thenReturn(0L);
    }

    @Test
    void shouldCreatePendingAmbassadorAndInvitedProfile() {
        when(companyRepository.findById(10L)).thenReturn(Optional.of(company));
        when(dashboardUserRepository.existsByUsername("sarah@example.com")).thenReturn(false);
        when(passwordEncoder.encode(any())).thenReturn("encoded-password");
        when(ambassadorProfileRepository.existsByAmbassadorCode(any())).thenReturn(false);
        when(dashboardUserRepository.save(any(DashboardUser.class))).thenAnswer(invocation -> {
            DashboardUser user = invocation.getArgument(0);
            user.setId(21L);
            return user;
        });
        when(ambassadorProfileRepository.save(any(AmbassadorProfile.class))).thenAnswer(invocation -> {
            AmbassadorProfile profile = invocation.getArgument(0);
            profile.setId(31L);
            return profile;
        });

        AmbassadorSummaryResponse response = ambassadorAdminService.createAmbassador(
                new CreateAmbassadorRequest(
                        "Sarah",
                        "Ahmed",
                        "sarah@example.com",
                        "4165551234",
                        "Sarah Travels",
                        "Instagram",
                        "@sarahtravels"
                )
        );

        ArgumentCaptor<DashboardUser> userCaptor = ArgumentCaptor.forClass(DashboardUser.class);
        verify(dashboardUserRepository).save(userCaptor.capture());
        DashboardUser savedUser = userCaptor.getValue();

        assertEquals(UserRole.AMBASSADOR, savedUser.getRole());
        assertEquals(UserStatus.PENDING, savedUser.getStatus());
        assertEquals("encoded-password", savedUser.getPassword());
        assertEquals("sarah@example.com", savedUser.getUsername());

        assertEquals(31L, response.id());
        assertEquals("Sarah", response.firstName());
        assertEquals(AmbassadorStatus.INVITED, response.status());
    }

    @Test
    void shouldDeactivateAmbassadorAndDisableReferralLinks() {
        DashboardUser user = new DashboardUser();
        user.setId(21L);
        user.setCompany(company);
        user.setFirstName("Sarah");
        user.setLastName("Ahmed");
        user.setUsername("sarah@example.com");
        user.setRole(UserRole.AMBASSADOR);
        user.setStatus(UserStatus.ACTIVE);

        AmbassadorProfile profile = new AmbassadorProfile();
        profile.setId(31L);
        profile.setCompany(company);
        profile.setUser(user);
        profile.setStatus(AmbassadorStatus.ACTIVE);
        profile.setAmbassadorCode("AMB-12345678");

        ReferralLink link = new ReferralLink();
        link.setStatus(ReferralLinkStatus.ACTIVE);
        link.setClickCount(3L);

        when(ambassadorProfileRepository.findDetailedByIdAndCompanyId(31L, 10L)).thenReturn(Optional.of(profile));
        when(referralLinkRepository.findByAmbassadorUserIdAndCompanyIdAndStatus(21L, 10L, ReferralLinkStatus.ACTIVE))
                .thenReturn(List.of(link));
        when(referralLinkRepository.findByAmbassadorUserIdAndCompanyId(21L, 10L)).thenReturn(List.of());

        AmbassadorDetailResponse response = ambassadorAdminService.deactivateAmbassador(31L);

        assertEquals(AmbassadorStatus.INACTIVE, profile.getStatus());
        assertEquals(UserStatus.INACTIVE, user.getStatus());
        assertEquals(ReferralLinkStatus.DISABLED, link.getStatus());
        assertEquals(AmbassadorStatus.INACTIVE, response.status());
        assertTrue(response.referralLinks().isEmpty());
    }
}
