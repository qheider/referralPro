package com.actpro.referral.ambassador;

import com.actpro.referral.ambassador.AmbassadorAdminService.AmbassadorProvisioningResult;
import com.actpro.referral.ambassador.dto.AmbassadorApplicationApprovalResponse;
import com.actpro.referral.ambassador.dto.AmbassadorApplicationDetailResponse;
import com.actpro.referral.ambassador.dto.AmbassadorApplicationSubmissionResponse;
import com.actpro.referral.ambassador.dto.AmbassadorSummaryResponse;
import com.actpro.referral.ambassador.dto.RejectApplicationRequest;
import com.actpro.referral.ambassador.dto.SubmitAmbassadorApplicationRequest;
import com.actpro.referral.auth.DashboardUser;
import com.actpro.referral.auth.DashboardUserRepository;
import com.actpro.referral.auth.dto.IssuedInvitationResponse;
import com.actpro.referral.common.exception.BadRequestException;
import com.actpro.referral.common.exception.NotFoundException;
import com.actpro.referral.company.Company;
import com.actpro.referral.company.CompanyRepository;
import com.actpro.referral.company.CompanyStatus;
import com.actpro.referral.outbox.OutboxEventPublisher;
import com.actpro.referral.security.CurrentUserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AmbassadorApplicationServiceTest {

    @Mock
    private AmbassadorApplicationRepository ambassadorApplicationRepository;

    @Mock
    private CompanyRepository companyRepository;

    @Mock
    private DashboardUserRepository dashboardUserRepository;

    @Mock
    private AmbassadorAdminService ambassadorAdminService;

    @Mock
    private CurrentUserService currentUserService;

    @Mock
    private OutboxEventPublisher outboxEventPublisher;

    @InjectMocks
    private AmbassadorApplicationService ambassadorApplicationService;

    private Company company;

    @BeforeEach
    void setUp() {
        company = new Company();
        company.setId(10L);
        company.setName("Acme Rentals");
        company.setStatus(CompanyStatus.ACTIVE);

        lenient().when(currentUserService.getCurrentCompanyId()).thenReturn(10L);
    }

    private SubmitAmbassadorApplicationRequest validRequest() {
        return new SubmitAmbassadorApplicationRequest(
                "Sarah",
                "Ahmed",
                "sarah@example.com",
                "4165551234",
                "Sarah Travels",
                "I'd love to spread the word!",
                "Instagram",
                "@sarahtravels"
        );
    }

    private AmbassadorApplication pendingApplication() {
        AmbassadorApplication application = new AmbassadorApplication();
        application.setId(41L);
        application.setCompany(company);
        application.setFirstName("Sarah");
        application.setLastName("Ahmed");
        application.setEmail("sarah@example.com");
        application.setBio("I'd love to spread the word!");
        application.setStatus(ApplicationStatus.PENDING);
        return application;
    }

    @Test
    void shouldSubmitApplicationAndPublishOutboxEvent() {
        when(companyRepository.findById(10L)).thenReturn(Optional.of(company));
        when(dashboardUserRepository.existsByUsername("sarah@example.com")).thenReturn(false);
        when(ambassadorApplicationRepository.existsByCompanyIdAndEmailAndStatus(10L, "sarah@example.com", ApplicationStatus.PENDING))
                .thenReturn(false);
        when(ambassadorApplicationRepository.save(any(AmbassadorApplication.class))).thenAnswer(invocation -> {
            AmbassadorApplication application = invocation.getArgument(0);
            application.setId(41L);
            return application;
        });

        AmbassadorApplicationSubmissionResponse response = ambassadorApplicationService.submitApplication(10L, validRequest());

        ArgumentCaptor<AmbassadorApplication> captor = ArgumentCaptor.forClass(AmbassadorApplication.class);
        verify(ambassadorApplicationRepository).save(captor.capture());
        AmbassadorApplication saved = captor.getValue();

        assertEquals(ApplicationStatus.PENDING, saved.getStatus());
        assertEquals("sarah@example.com", saved.getEmail());
        assertEquals(41L, response.applicationId());
        assertEquals(ApplicationStatus.PENDING, response.status());

        verify(outboxEventPublisher).publish(eq(company), eq("AMBASSADOR_APPLICATION"), eq(41L),
                eq("ambassador_application.submitted"), any());
    }

    @Test
    void shouldRejectSubmissionWhenCompanyNotFound() {
        when(companyRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> ambassadorApplicationService.submitApplication(99L, validRequest()));
        verify(ambassadorApplicationRepository, never()).save(any());
    }

    @Test
    void shouldRejectSubmissionWhenCompanyNotActive() {
        company.setStatus(CompanyStatus.SUSPENDED);
        when(companyRepository.findById(10L)).thenReturn(Optional.of(company));

        assertThrows(BadRequestException.class, () -> ambassadorApplicationService.submitApplication(10L, validRequest()));
        verify(ambassadorApplicationRepository, never()).save(any());
    }

    @Test
    void shouldRejectSubmissionWhenEmailAlreadyHasAnAccount() {
        when(companyRepository.findById(10L)).thenReturn(Optional.of(company));
        when(dashboardUserRepository.existsByUsername("sarah@example.com")).thenReturn(true);

        assertThrows(BadRequestException.class, () -> ambassadorApplicationService.submitApplication(10L, validRequest()));
        verify(ambassadorApplicationRepository, never()).save(any());
        verify(outboxEventPublisher, never()).publish(any(), any(), any(), any(), any());
    }

    @Test
    void shouldRejectSubmissionWhenPendingApplicationAlreadyExistsForEmail() {
        when(companyRepository.findById(10L)).thenReturn(Optional.of(company));
        when(dashboardUserRepository.existsByUsername("sarah@example.com")).thenReturn(false);
        when(ambassadorApplicationRepository.existsByCompanyIdAndEmailAndStatus(10L, "sarah@example.com", ApplicationStatus.PENDING))
                .thenReturn(true);

        assertThrows(BadRequestException.class, () -> ambassadorApplicationService.submitApplication(10L, validRequest()));
        verify(ambassadorApplicationRepository, never()).save(any());
    }

    @Test
    void shouldApproveApplicationAndProvisionAmbassadorAccount() {
        AmbassadorApplication application = pendingApplication();
        when(ambassadorApplicationRepository.findByIdAndCompanyId(41L, 10L)).thenReturn(Optional.of(application));

        DashboardUser user = new DashboardUser();
        user.setId(21L);
        user.setCompany(company);

        AmbassadorProfile profile = new AmbassadorProfile();
        profile.setId(31L);
        profile.setUser(user);
        profile.setCompany(company);
        profile.setStatus(AmbassadorStatus.INVITED);

        IssuedInvitationResponse invitation = new IssuedInvitationResponse(101L, "raw-invitation-token", LocalDateTime.now().plusDays(7));
        AmbassadorProvisioningResult provisioningResult = new AmbassadorProvisioningResult(profile, invitation);

        when(ambassadorAdminService.provisionAmbassadorAccount(
                eq(company), eq("sarah@example.com"), eq("Sarah"), eq("Ahmed"), any(), any(),
                eq("I'd love to spread the word!"), any(), any()))
                .thenReturn(provisioningResult);
        when(currentUserService.getCurrentUserId()).thenReturn(5L);
        when(ambassadorAdminService.toSummary(profile)).thenReturn(new AmbassadorSummaryResponse(
                31L, 21L, "Sarah", "Ahmed", "sarah@example.com", null, AmbassadorStatus.INVITED, 0L, 0L, 0L, 0.0, LocalDateTime.now()));

        AmbassadorApplicationApprovalResponse response = ambassadorApplicationService.approveApplication(41L);

        assertEquals(ApplicationStatus.APPROVED, application.getStatus());
        assertEquals(5L, application.getReviewedByUserId());
        assertEquals(31L, application.getResultingAmbassadorProfileId());
        assertEquals("raw-invitation-token", response.invitationToken());
        assertEquals(31L, response.ambassador().id());

        verify(outboxEventPublisher).publish(eq(company), eq("AMBASSADOR_APPLICATION"), eq(41L),
                eq("ambassador_application.approved"), any());
    }

    @Test
    void shouldRejectApprovalWhenApplicationNotPending() {
        AmbassadorApplication application = pendingApplication();
        application.setStatus(ApplicationStatus.APPROVED);
        when(ambassadorApplicationRepository.findByIdAndCompanyId(41L, 10L)).thenReturn(Optional.of(application));

        assertThrows(BadRequestException.class, () -> ambassadorApplicationService.approveApplication(41L));
        verify(ambassadorAdminService, never()).provisionAmbassadorAccount(any(), any(), any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    void shouldRejectApprovalWhenCompanyNotActive() {
        company.setStatus(CompanyStatus.SUSPENDED);
        AmbassadorApplication application = pendingApplication();
        when(ambassadorApplicationRepository.findByIdAndCompanyId(41L, 10L)).thenReturn(Optional.of(application));

        assertThrows(BadRequestException.class, () -> ambassadorApplicationService.approveApplication(41L));
        verify(ambassadorAdminService, never()).provisionAmbassadorAccount(any(), any(), any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    void shouldLeaveApplicationPendingWhenProvisioningFailsDueToEmailConflict() {
        AmbassadorApplication application = pendingApplication();
        when(ambassadorApplicationRepository.findByIdAndCompanyId(41L, 10L)).thenReturn(Optional.of(application));
        when(ambassadorAdminService.provisionAmbassadorAccount(any(), any(), any(), any(), any(), any(), any(), any(), any()))
                .thenThrow(new BadRequestException("Email is already in use"));

        assertThrows(BadRequestException.class, () -> ambassadorApplicationService.approveApplication(41L));

        assertEquals(ApplicationStatus.PENDING, application.getStatus());
        assertNull(application.getReviewedByUserId());
        verify(outboxEventPublisher, never()).publish(any(), any(), any(), eq("ambassador_application.approved"), any());
    }

    @Test
    void shouldRejectApplicationAndPublishOutbox() {
        AmbassadorApplication application = pendingApplication();
        when(ambassadorApplicationRepository.findByIdAndCompanyId(41L, 10L)).thenReturn(Optional.of(application));
        when(currentUserService.getCurrentUserId()).thenReturn(5L);

        AmbassadorApplicationDetailResponse response = ambassadorApplicationService.rejectApplication(
                41L, new RejectApplicationRequest("Not a good fit right now"));

        assertEquals(ApplicationStatus.REJECTED, application.getStatus());
        assertEquals("Not a good fit right now", application.getRejectionReason());
        assertEquals(5L, application.getReviewedByUserId());
        assertEquals(ApplicationStatus.REJECTED, response.status());

        verify(outboxEventPublisher).publish(eq(company), eq("AMBASSADOR_APPLICATION"), eq(41L),
                eq("ambassador_application.rejected"), any());
    }

    @Test
    void shouldRejectRejectionWhenApplicationNotPending() {
        AmbassadorApplication application = pendingApplication();
        application.setStatus(ApplicationStatus.REJECTED);
        when(ambassadorApplicationRepository.findByIdAndCompanyId(41L, 10L)).thenReturn(Optional.of(application));

        assertThrows(BadRequestException.class, () ->
                ambassadorApplicationService.rejectApplication(41L, new RejectApplicationRequest("Already handled")));
    }

    @Test
    void shouldNotFindApplicationFromAnotherCompany() {
        when(ambassadorApplicationRepository.findByIdAndCompanyId(41L, 10L)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> ambassadorApplicationService.getApplication(41L));
    }
}
