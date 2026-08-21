package com.actpro.referral.ambassador;

import com.actpro.referral.ambassador.AmbassadorAdminService.AmbassadorProvisioningResult;
import com.actpro.referral.ambassador.dto.*;
import com.actpro.referral.auth.DashboardUser;
import com.actpro.referral.auth.DashboardUserRepository;
import com.actpro.referral.auth.UserRole;
import com.actpro.referral.campaign.Campaign;
import com.actpro.referral.campaign.CampaignService;
import com.actpro.referral.common.EmailService;
import com.actpro.referral.common.exception.BadRequestException;
import com.actpro.referral.common.exception.NotFoundException;
import com.actpro.referral.company.Company;
import com.actpro.referral.company.CompanyRepository;
import com.actpro.referral.company.CompanyStatus;
import com.actpro.referral.outbox.OutboxEventPublisher;
import com.actpro.referral.security.CurrentUserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;

@Service
@RequiredArgsConstructor
@Slf4j
public class AmbassadorApplicationService {

    private static final String AGGREGATE_TYPE = "AMBASSADOR_APPLICATION";

    private final AmbassadorApplicationRepository ambassadorApplicationRepository;
    private final CompanyRepository companyRepository;
    private final DashboardUserRepository dashboardUserRepository;
    private final AmbassadorAdminService ambassadorAdminService;
    private final CampaignService campaignService;
    private final CurrentUserService currentUserService;
    private final OutboxEventPublisher outboxEventPublisher;
    private final EmailService emailService;

    /**
     * Public, unauthenticated entry point - there is no principal yet, so unlike everywhere else
     * in this codebase, companyId is a deliberate, untrusted, client-supplied value rather than
     * something checked against CurrentUserService. Don't "fix" this into requiring auth: an
     * applicant has no account until an admin approves them.
     * <p>
     * {@code campaignCode} is optional: present when the applicant came through a campaign's
     * public join link (Phase 3), null for the company-wide admin-invited path. When present it
     * is hard-validated (enrollment must actually be open) via CampaignService.getCampaignForEnrollment
     * and stored on the application so the resulting ambassador is auto-assigned to that campaign
     * once they accept their invitation (see AmbassadorAdminService.activateInvitedAmbassador).
     */
    @Transactional
    public AmbassadorApplicationSubmissionResponse submitApplication(
            Long companyId, String campaignCode, SubmitAmbassadorApplicationRequest request) {
        Company company = companyRepository.findById(companyId)
                .orElseThrow(() -> new NotFoundException("Company not found"));

        if (company.getStatus() != CompanyStatus.ACTIVE) {
            throw new BadRequestException("Company is not accepting ambassador applications");
        }

        Campaign campaign = null;
        if (campaignCode != null && !campaignCode.isBlank()) {
            campaign = campaignService.getCampaignForEnrollment(campaignCode, company);
        }

        String normalizedEmail = request.email().trim().toLowerCase(Locale.ROOT);

        if (dashboardUserRepository.existsByUsername(normalizedEmail)) {
            throw new BadRequestException("An account with this email already exists");
        }

        if (ambassadorApplicationRepository.existsByCompanyIdAndEmailAndStatus(companyId, normalizedEmail, ApplicationStatus.PENDING)) {
            throw new BadRequestException("An application for this email is already pending review");
        }

        AmbassadorApplication application = new AmbassadorApplication();
        application.setCompany(company);
        application.setCampaign(campaign);
        application.setFirstName(request.firstName().trim());
        application.setLastName(request.lastName().trim());
        application.setEmail(normalizedEmail);
        application.setPhone(normalizeNullable(request.phone()));
        application.setDisplayName(normalizeNullable(request.displayName()));
        application.setBio(normalizeNullable(request.bio()));
        application.setSocialMediaPlatform(normalizeNullable(request.socialMediaPlatform()));
        application.setSocialMediaHandle(normalizeNullable(request.socialMediaHandle()));
        application.setStatus(ApplicationStatus.PENDING);
        application = ambassadorApplicationRepository.save(application);

        notifyOnSubmission(company, application);
        publishEvent(company, application, "ambassador_application.submitted");

        return new AmbassadorApplicationSubmissionResponse(application.getId(), application.getStatus(), application.getCreatedAt());
    }

    /**
     * Best-effort - same "don't let a mail-server hiccup roll back real state" pattern as
     * AmbassadorAdminService.provisionAmbassadorAccount and CompanyService.registerCompany. Notifies
     * every COMPANY_ADMIN for the company (not just one), since any of them may need to act on it.
     */
    private void notifyOnSubmission(Company company, AmbassadorApplication application) {
        String applicantName = application.getFirstName() + " " + application.getLastName();

        try {
            emailService.sendAmbassadorApplicationReceivedEmail(application.getEmail(), applicantName, company.getName());
        } catch (Exception e) {
            log.warn("Failed to send application-received email, but the application was submitted.", e);
        }

        List<DashboardUser> admins = dashboardUserRepository.findByCompanyIdAndRole(company.getId(), UserRole.COMPANY_ADMIN);
        for (DashboardUser admin : admins) {
            try {
                emailService.sendAmbassadorApplicationAdminNotificationEmail(
                        admin.getUsername(), applicantName, application.getEmail(), company.getName());
            } catch (Exception e) {
                log.warn("Failed to send application admin-notification email to {}, but the application was submitted.",
                        admin.getUsername(), e);
            }
        }
    }

    @Transactional(readOnly = true)
    public AmbassadorApplicationPageResponse listApplications(int page, int size, String sort, String search, ApplicationStatus status) {
        Pageable pageable = PageRequest.of(page, size, parseSort(sort));
        Page<AmbassadorApplication> result = ambassadorApplicationRepository.searchByCompanyId(
                currentUserService.getCurrentCompanyId(),
                normalizeSearch(search),
                status,
                pageable
        );

        List<AmbassadorApplicationSummaryResponse> content = result.getContent().stream()
                .map(this::toSummary)
                .toList();

        return new AmbassadorApplicationPageResponse(
                content,
                result.getNumber(),
                result.getSize(),
                result.getTotalElements(),
                result.getTotalPages(),
                result.isFirst(),
                result.isLast()
        );
    }

    @Transactional(readOnly = true)
    public AmbassadorApplicationDetailResponse getApplication(Long applicationId) {
        return toDetail(findOrThrow(applicationId));
    }

    @Transactional
    public AmbassadorApplicationApprovalResponse approveApplication(Long applicationId) {
        AmbassadorApplication application = findOrThrow(applicationId);

        if (application.getStatus() != ApplicationStatus.PENDING) {
            throw new BadRequestException("Only pending applications can be approved");
        }

        Company company = application.getCompany();
        if (company.getStatus() != CompanyStatus.ACTIVE) {
            throw new BadRequestException("Company must be active to approve ambassadors");
        }

        // Provision the account before mutating the application at all. If the applicant's email
        // has since been claimed by a real DashboardUser, provisionAmbassadorAccount throws here -
        // nothing on `application` has been touched yet, so this @Transactional method rolls back
        // to leave the application exactly PENDING rather than silently flipping it to REJECTED.
        AmbassadorProvisioningResult result = ambassadorAdminService.provisionAmbassadorAccount(
                company,
                application.getEmail(),
                application.getFirstName(),
                application.getLastName(),
                application.getDisplayName(),
                application.getPhone(),
                application.getBio(),
                application.getSocialMediaPlatform(),
                application.getSocialMediaHandle()
        );

        application.setStatus(ApplicationStatus.APPROVED);
        application.setReviewedAt(LocalDateTime.now());
        application.setReviewedByUserId(currentUserService.getCurrentUserId());
        application.setResultingAmbassadorProfileId(result.profile().getId());

        publishEvent(company, application, "ambassador_application.approved");

        return new AmbassadorApplicationApprovalResponse(
                toDetail(application),
                ambassadorAdminService.toSummary(result.profile()),
                result.invitation().token(),
                result.invitation().expiresAt()
        );
    }

    @Transactional
    public AmbassadorApplicationDetailResponse rejectApplication(Long applicationId, RejectApplicationRequest request) {
        AmbassadorApplication application = findOrThrow(applicationId);

        if (application.getStatus() != ApplicationStatus.PENDING) {
            throw new BadRequestException("Only pending applications can be rejected");
        }

        application.setStatus(ApplicationStatus.REJECTED);
        application.setRejectionReason(request.reason().trim());
        application.setReviewedAt(LocalDateTime.now());
        application.setReviewedByUserId(currentUserService.getCurrentUserId());

        publishEvent(application.getCompany(), application, "ambassador_application.rejected");

        return toDetail(application);
    }

    private void publishEvent(Company company, AmbassadorApplication application, String eventType) {
        AmbassadorApplicationEventPayload payload = new AmbassadorApplicationEventPayload(
                application.getId(),
                company.getId(),
                application.getEmail(),
                application.getFirstName(),
                application.getLastName(),
                application.getStatus(),
                application.getRejectionReason(),
                LocalDateTime.now()
        );
        outboxEventPublisher.publish(company, AGGREGATE_TYPE, application.getId(), eventType, payload);
    }

    private AmbassadorApplication findOrThrow(Long applicationId) {
        return ambassadorApplicationRepository.findByIdAndCompanyId(applicationId, currentUserService.getCurrentCompanyId())
                .orElseThrow(() -> new NotFoundException("Ambassador application not found"));
    }

    private AmbassadorApplicationSummaryResponse toSummary(AmbassadorApplication application) {
        return new AmbassadorApplicationSummaryResponse(
                application.getId(),
                application.getFirstName(),
                application.getLastName(),
                application.getEmail(),
                application.getDisplayName(),
                application.getCampaign() != null ? application.getCampaign().getId() : null,
                application.getCampaign() != null ? application.getCampaign().getName() : null,
                application.getStatus(),
                application.getCreatedAt(),
                application.getReviewedAt()
        );
    }

    private AmbassadorApplicationDetailResponse toDetail(AmbassadorApplication application) {
        return new AmbassadorApplicationDetailResponse(
                application.getId(),
                application.getFirstName(),
                application.getLastName(),
                application.getEmail(),
                application.getPhone(),
                application.getDisplayName(),
                application.getBio(),
                application.getSocialMediaPlatform(),
                application.getSocialMediaHandle(),
                application.getCampaign() != null ? application.getCampaign().getId() : null,
                application.getCampaign() != null ? application.getCampaign().getName() : null,
                application.getStatus(),
                application.getRejectionReason(),
                application.getReviewedByUserId(),
                application.getReviewedAt(),
                application.getResultingAmbassadorProfileId(),
                application.getCreatedAt()
        );
    }

    private String normalizeSearch(String value) {
        String normalized = normalizeNullable(value);
        return normalized == null ? null : normalized.toLowerCase(Locale.ROOT);
    }

    private String normalizeNullable(String value) {
        if (value == null) {
            return null;
        }

        String normalized = value.trim();
        return normalized.isEmpty() ? null : normalized;
    }

    private Sort parseSort(String sort) {
        if (sort == null || sort.isBlank()) {
            return Sort.by(Sort.Direction.DESC, "createdAt");
        }

        String[] parts = sort.split(",", 2);
        String property = switch (parts[0].trim()) {
            case "firstName" -> "firstName";
            case "lastName" -> "lastName";
            case "status" -> "status";
            case "email" -> "email";
            default -> "createdAt";
        };
        Sort.Direction direction = parts.length > 1 && "asc".equalsIgnoreCase(parts[1].trim())
                ? Sort.Direction.ASC
                : Sort.Direction.DESC;

        return Sort.by(direction, property);
    }

    private record AmbassadorApplicationEventPayload(
            Long applicationId,
            Long companyId,
            String email,
            String firstName,
            String lastName,
            ApplicationStatus status,
            String rejectionReason,
            LocalDateTime occurredAt
    ) {
    }
}
