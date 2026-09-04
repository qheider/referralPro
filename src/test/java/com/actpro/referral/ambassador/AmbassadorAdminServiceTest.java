package com.actpro.referral.ambassador;

import com.actpro.referral.ambassador.AmbassadorAdminService.AmbassadorProvisioningResult;
import com.actpro.referral.ambassador.dto.AmbassadorCreationResponse;
import com.actpro.referral.ambassador.dto.AmbassadorDetailResponse;
import com.actpro.referral.ambassador.dto.CreateAmbassadorRequest;
import com.actpro.referral.auth.AccountInvitationService;
import com.actpro.referral.auth.DashboardUser;
import com.actpro.referral.auth.DashboardUserRepository;
import com.actpro.referral.auth.InvitationPurpose;
import com.actpro.referral.auth.UserRole;
import com.actpro.referral.auth.UserStatus;
import com.actpro.referral.auth.dto.IssuedInvitationResponse;
import com.actpro.referral.common.EmailService;
import com.actpro.referral.common.exception.BadRequestException;
import com.actpro.referral.common.exception.NotFoundException;
import com.actpro.referral.company.Company;
import com.actpro.referral.company.CompanyRepository;
import com.actpro.referral.company.CompanyStatus;
import com.actpro.referral.referral.ReferralLink;
import com.actpro.referral.referral.ReferralLinkRepository;
import com.actpro.referral.referral.ReferralLinkStatus;
import com.actpro.referral.referral.ReferralLinkUrlService;
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

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
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
    private ReferralLinkUrlService referralLinkUrlService;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private CurrentUserService currentUserService;

    @Mock
    private AccountInvitationService accountInvitationService;

    @Mock
    private AmbassadorApplicationRepository ambassadorApplicationRepository;

    @Mock
    private CampaignAssignmentService campaignAssignmentService;

    @Mock
    private EmailService emailService;

    @InjectMocks
    private AmbassadorAdminService ambassadorAdminService;

    private Company company;

    @BeforeEach
    void setUp() {
        company = new Company();
        company.setId(10L);
        company.setName("Acme Rentals");
        company.setStatus(CompanyStatus.ACTIVE);

        lenient().when(currentUserService.getCurrentCompanyId()).thenReturn(10L);
        lenient().when(assignmentRepository.countByAmbassadorUserIdAndCompanyIdAndStatus(any(), any(), any())).thenReturn(0L);
        lenient().when(referralRepository.countByAmbassadorUserIdAndCompanyIdAndStatusIn(any(), any(), any())).thenReturn(0L);
        lenient().when(referralRepository.countByAmbassadorUserIdAndCompanyIdAndStatus(any(), any(), any())).thenReturn(0L);
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
        when(accountInvitationService.issueInvitation(any(DashboardUser.class), eq(InvitationPurpose.AMBASSADOR_ONBOARDING)))
                .thenReturn(new IssuedInvitationResponse(100L, "raw-invitation-token", LocalDateTime.now().plusDays(7)));

        AmbassadorCreationResponse response = ambassadorAdminService.createAmbassador(
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

        assertEquals(31L, response.ambassador().id());
        assertEquals("Sarah", response.ambassador().firstName());
        assertEquals(AmbassadorStatus.INVITED, response.ambassador().status());
        assertEquals("raw-invitation-token", response.invitationToken());

        verify(emailService).sendAmbassadorInvitationEmail("sarah@example.com", "raw-invitation-token", "Sarah Ahmed");
    }

    @Test
    void shouldProvisionAmbassadorAccountReusableByApplicationApproval() {
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
        when(accountInvitationService.issueInvitation(any(DashboardUser.class), eq(InvitationPurpose.AMBASSADOR_ONBOARDING)))
                .thenReturn(new IssuedInvitationResponse(100L, "raw-invitation-token", LocalDateTime.now().plusDays(7)));

        AmbassadorProvisioningResult result = ambassadorAdminService.provisionAmbassadorAccount(
                company, "sarah@example.com", "Sarah", "Ahmed", "Sarah Travels", "4165551234",
                "Applicant's motivation message", "Instagram", "@sarahtravels");

        assertEquals("Applicant's motivation message", result.profile().getBio());
        assertEquals(AmbassadorStatus.INVITED, result.profile().getStatus());
        assertEquals("raw-invitation-token", result.invitation().token());
    }

    @Test
    void shouldRejectProvisioningWhenEmailAlreadyInUse() {
        when(dashboardUserRepository.existsByUsername("sarah@example.com")).thenReturn(true);

        assertThrows(BadRequestException.class, () -> ambassadorAdminService.provisionAmbassadorAccount(
                company, "sarah@example.com", "Sarah", "Ahmed", null, null, null, null, null));

        verify(dashboardUserRepository, never()).save(any());
        verify(accountInvitationService, never()).issueInvitation(any(), any());
    }

    @Test
    void shouldResendInvitationForOutstandingAmbassador() {
        DashboardUser user = new DashboardUser();
        user.setId(21L);
        user.setCompany(company);

        AmbassadorProfile profile = new AmbassadorProfile();
        profile.setId(31L);
        profile.setCompany(company);
        profile.setUser(user);
        profile.setStatus(AmbassadorStatus.INVITED);

        when(ambassadorProfileRepository.findDetailedByIdAndCompanyId(31L, 10L)).thenReturn(Optional.of(profile));
        when(accountInvitationService.issueInvitation(user, InvitationPurpose.AMBASSADOR_ONBOARDING))
                .thenReturn(new IssuedInvitationResponse(101L, "resent-token", LocalDateTime.now().plusDays(7)));

        IssuedInvitationResponse response = ambassadorAdminService.resendInvitation(31L);

        assertEquals("resent-token", response.token());
    }

    @Test
    void shouldRejectResendingInvitationForAlreadyActiveAmbassador() {
        DashboardUser user = new DashboardUser();
        user.setId(21L);
        user.setCompany(company);

        AmbassadorProfile profile = new AmbassadorProfile();
        profile.setId(31L);
        profile.setCompany(company);
        profile.setUser(user);
        profile.setStatus(AmbassadorStatus.ACTIVE);

        when(ambassadorProfileRepository.findDetailedByIdAndCompanyId(31L, 10L)).thenReturn(Optional.of(profile));

        assertThrows(BadRequestException.class, () -> ambassadorAdminService.resendInvitation(31L));
        verify(accountInvitationService, never()).issueInvitation(any(), any());
    }

    @Test
    void shouldActivateInvitedAmbassadorAfterInvitationAccepted() {
        DashboardUser user = new DashboardUser();
        user.setId(21L);
        user.setCompany(company);
        user.setStatus(UserStatus.PENDING);

        AmbassadorProfile profile = new AmbassadorProfile();
        profile.setId(31L);
        profile.setCompany(company);
        profile.setUser(user);
        profile.setStatus(AmbassadorStatus.INVITED);

        when(ambassadorProfileRepository.findByUserId(21L)).thenReturn(Optional.of(profile));
        when(referralLinkRepository.findByAmbassadorUserIdAndCompanyIdAndStatus(21L, 10L, ReferralLinkStatus.DISABLED))
                .thenReturn(List.of());

        ambassadorAdminService.activateInvitedAmbassador(21L);

        assertEquals(AmbassadorStatus.ACTIVE, profile.getStatus());
    }

    @Test
    void shouldAutoAssignToCampaignWhenActivatedFromCampaignScopedApplication() {
        DashboardUser user = new DashboardUser();
        user.setId(21L);
        user.setCompany(company);
        user.setStatus(UserStatus.PENDING);

        AmbassadorProfile profile = new AmbassadorProfile();
        profile.setId(31L);
        profile.setCompany(company);
        profile.setUser(user);
        profile.setStatus(AmbassadorStatus.INVITED);

        com.actpro.referral.campaign.Campaign campaign = new com.actpro.referral.campaign.Campaign();
        campaign.setId(77L);
        campaign.setCompany(company);

        AmbassadorApplication application = new AmbassadorApplication();
        application.setId(41L);
        application.setCompany(company);
        application.setCampaign(campaign);
        application.setReviewedByUserId(5L);

        DashboardUser reviewer = new DashboardUser();
        reviewer.setId(5L);

        when(ambassadorProfileRepository.findByUserId(21L)).thenReturn(Optional.of(profile));
        when(referralLinkRepository.findByAmbassadorUserIdAndCompanyIdAndStatus(21L, 10L, ReferralLinkStatus.DISABLED))
                .thenReturn(List.of());
        when(ambassadorApplicationRepository.findByResultingAmbassadorProfileId(31L)).thenReturn(Optional.of(application));
        when(dashboardUserRepository.findById(5L)).thenReturn(Optional.of(reviewer));

        ambassadorAdminService.activateInvitedAmbassador(21L);

        assertEquals(AmbassadorStatus.ACTIVE, profile.getStatus());
        verify(campaignAssignmentService).autoAssignFromApplication(campaign, profile, reviewer);
    }

    @Test
    void shouldNotAutoAssignWhenActivatedApplicationHasNoCampaign() {
        DashboardUser user = new DashboardUser();
        user.setId(21L);
        user.setCompany(company);
        user.setStatus(UserStatus.PENDING);

        AmbassadorProfile profile = new AmbassadorProfile();
        profile.setId(31L);
        profile.setCompany(company);
        profile.setUser(user);
        profile.setStatus(AmbassadorStatus.INVITED);

        AmbassadorApplication application = new AmbassadorApplication();
        application.setId(41L);
        application.setCompany(company);

        when(ambassadorProfileRepository.findByUserId(21L)).thenReturn(Optional.of(profile));
        when(referralLinkRepository.findByAmbassadorUserIdAndCompanyIdAndStatus(21L, 10L, ReferralLinkStatus.DISABLED))
                .thenReturn(List.of());
        when(ambassadorApplicationRepository.findByResultingAmbassadorProfileId(31L)).thenReturn(Optional.of(application));

        ambassadorAdminService.activateInvitedAmbassador(21L);

        assertEquals(AmbassadorStatus.ACTIVE, profile.getStatus());
        verify(campaignAssignmentService, never()).autoAssignFromApplication(any(), any(), any());
    }

    @Test
    void shouldRejectActivatingUnknownUser() {
        when(ambassadorProfileRepository.findByUserId(99L)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> ambassadorAdminService.activateInvitedAmbassador(99L));
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

    @Test
    void shouldIncludeReferralUrlAndQrCodeUrlInAmbassadorDetail() {
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

        com.actpro.referral.campaign.Campaign campaign = new com.actpro.referral.campaign.Campaign();
        campaign.setId(40L);
        campaign.setName("Summer campaign");

        ReferralLink link = new ReferralLink();
        link.setCampaign(campaign);
        link.setPublicToken("tok-123");
        link.setStatus(ReferralLinkStatus.ACTIVE);
        link.setClickCount(3L);

        when(ambassadorProfileRepository.findDetailedByIdAndCompanyId(31L, 10L)).thenReturn(Optional.of(profile));
        when(referralLinkRepository.findByAmbassadorUserIdAndCompanyId(21L, 10L)).thenReturn(List.of(link));
        when(currentUserService.getCurrentCompanyId()).thenReturn(10L);
        when(referralLinkUrlService.resolveReferralUrl(link)).thenReturn("http://localhost:8080/r/tok-123");
        when(referralLinkUrlService.resolveQrCodeUrl(link)).thenReturn("http://localhost:8080/r/link/tok-123/qrcode");

        AmbassadorDetailResponse response = ambassadorAdminService.getAmbassador(31L);

        assertEquals(1, response.referralLinks().size());
        assertEquals("http://localhost:8080/r/tok-123", response.referralLinks().get(0).referralUrl());
        assertEquals("http://localhost:8080/r/link/tok-123/qrcode", response.referralLinks().get(0).qrCodeUrl());
    }
}
