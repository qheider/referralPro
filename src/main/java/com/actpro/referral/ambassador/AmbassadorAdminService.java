package com.actpro.referral.ambassador;

import com.actpro.referral.ambassador.dto.*;
import com.actpro.referral.auth.AccountInvitationService;
import com.actpro.referral.auth.DashboardUser;
import com.actpro.referral.auth.DashboardUserRepository;
import com.actpro.referral.common.EmailService;
import com.actpro.referral.auth.InvitationPurpose;
import com.actpro.referral.auth.UserRole;
import com.actpro.referral.auth.UserStatus;
import com.actpro.referral.auth.dto.IssuedInvitationResponse;
import com.actpro.referral.common.exception.BadRequestException;
import com.actpro.referral.common.exception.NotFoundException;
import com.actpro.referral.company.Company;
import com.actpro.referral.company.CompanyRepository;
import com.actpro.referral.company.CompanyStatus;
import com.actpro.referral.referral.Referral;
import com.actpro.referral.referral.ReferralLink;
import com.actpro.referral.referral.ReferralLinkRepository;
import com.actpro.referral.referral.ReferralLinkStatus;
import com.actpro.referral.referral.ReferralRepository;
import com.actpro.referral.referral.ReferralStatus;
import com.actpro.referral.security.CurrentUserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class AmbassadorAdminService {

    private static final String TOKEN_ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
    // Never presented to anyone - DashboardUser.password can't be null, but a real credential is
    // only ever set via AccountInvitationService.acceptInvitation. This is just filler.
    private static final String PLACEHOLDER_PASSWORD_ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz23456789!@#$%";
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final AmbassadorProfileRepository ambassadorProfileRepository;
    private final CampaignAmbassadorAssignmentRepository assignmentRepository;
    private final AmbassadorApplicationRepository ambassadorApplicationRepository;
    private final DashboardUserRepository dashboardUserRepository;
    private final CompanyRepository companyRepository;
    private final ReferralRepository referralRepository;
    private final ReferralLinkRepository referralLinkRepository;
    private final PasswordEncoder passwordEncoder;
    private final CurrentUserService currentUserService;
    private final AccountInvitationService accountInvitationService;
    private final CampaignAssignmentService campaignAssignmentService;
    private final EmailService emailService;

    @Transactional
    public AmbassadorCreationResponse createAmbassador(CreateAmbassadorRequest request) {
        Company company = getCurrentCompany();

        if (company.getStatus() != CompanyStatus.ACTIVE) {
            throw new BadRequestException("Company must be active to create ambassadors");
        }

        // Admin-direct creation has no bio field on CreateAmbassadorRequest, so it passes null -
        // AmbassadorApplicationService.approveApplication is the other caller of this helper and
        // passes the applicant's submitted bio through instead.
        AmbassadorProvisioningResult result = provisionAmbassadorAccount(
                company,
                request.email(),
                request.firstName(),
                request.lastName(),
                request.displayName(),
                request.phone(),
                null,
                request.socialMediaPlatform(),
                request.socialMediaHandle()
        );

        return new AmbassadorCreationResponse(toSummary(result.profile()), result.invitation().token(), result.invitation().expiresAt());
    }

    /**
     * Creates the DashboardUser + AmbassadorProfile pair and issues the onboarding invitation -
     * the reusable core of ambassador account creation, shared by the admin-direct createAmbassador
     * path and AmbassadorApplicationService.approveApplication. Callers own their own preconditions
     * (e.g. company-active checks) - this only knows how to provision one account.
     */
    AmbassadorProvisioningResult provisionAmbassadorAccount(
            Company company,
            String email,
            String firstName,
            String lastName,
            String displayName,
            String phone,
            String bio,
            String socialMediaPlatform,
            String socialMediaHandle
    ) {
        String normalizedEmail = email.trim().toLowerCase(Locale.ROOT);
        if (dashboardUserRepository.existsByUsername(normalizedEmail)) {
            throw new BadRequestException("Email is already in use");
        }

        DashboardUser user = new DashboardUser();
        user.setCompany(company);
        user.setUsername(normalizedEmail);
        user.setPassword(passwordEncoder.encode(generatePlaceholderPassword()));
        user.setFirstName(firstName.trim());
        user.setLastName(lastName.trim());
        user.setRole(UserRole.AMBASSADOR);
        user.setStatus(UserStatus.PENDING);
        user = dashboardUserRepository.save(user);

        AmbassadorProfile profile = new AmbassadorProfile();
        profile.setUser(user);
        profile.setCompany(company);
        profile.setDisplayName(normalizeNullable(displayName));
        profile.setPhone(normalizeNullable(phone));
        profile.setBio(normalizeNullable(bio));
        profile.setSocialMediaPlatform(normalizeNullable(socialMediaPlatform));
        profile.setSocialMediaHandle(normalizeNullable(socialMediaHandle));
        profile.setAmbassadorCode(generateAmbassadorCode());
        profile.setStatus(AmbassadorStatus.INVITED);
        profile = ambassadorProfileRepository.save(profile);

        IssuedInvitationResponse invitation = accountInvitationService.issueInvitation(user, InvitationPurpose.AMBASSADOR_ONBOARDING);
        // Don't let a mail-server hiccup roll back the ambassador account we just created - same
        // pattern as CompanyService.registerCompany's verification email. The admin can always
        // resend the invitation once mail is reachable.
        try {
            emailService.sendAmbassadorInvitationEmail(user.getUsername(), invitation.token(), user.getFirstName() + " " + user.getLastName());
        } catch (Exception e) {
            log.warn("Failed to send ambassador invitation email, but the ambassador account was created.", e);
        }

        return new AmbassadorProvisioningResult(profile, invitation);
    }

    record AmbassadorProvisioningResult(AmbassadorProfile profile, IssuedInvitationResponse invitation) {
    }

    @Transactional
    public IssuedInvitationResponse resendInvitation(Long ambassadorId) {
        AmbassadorProfile profile = findProfileOrThrow(ambassadorId);

        if (profile.getStatus() == AmbassadorStatus.ACTIVE) {
            throw new BadRequestException("Ambassador has already accepted an invitation");
        }

        IssuedInvitationResponse invitation = accountInvitationService.issueInvitation(profile.getUser(), InvitationPurpose.AMBASSADOR_ONBOARDING);
        DashboardUser user = profile.getUser();
        try {
            emailService.sendAmbassadorInvitationEmail(user.getUsername(), invitation.token(), user.getFirstName() + " " + user.getLastName());
        } catch (Exception e) {
            log.warn("Failed to send ambassador invitation email, but the invitation was reissued.", e);
        }
        return invitation;
    }

    /**
     * Called after a successful invitation acceptance (see AccountInvitationController) to flip
     * the AmbassadorProfile live. Deliberately not company-scoped: the caller has already
     * validated the invitation token and resolved a trustworthy user id from it, so there is no
     * client-supplied company id to check here.
     */
    @Transactional
    public void activateInvitedAmbassador(Long userId) {
        AmbassadorProfile profile = ambassadorProfileRepository.findByUserId(userId)
                .orElseThrow(() -> new NotFoundException("Ambassador not found"));
        applyStatus(profile, AmbassadorStatus.ACTIVE);

        // If this ambassador was provisioned via a campaign's public join-link application
        // (Phase 3/4), auto-assign them to that campaign now that they're active, rather than
        // leaving it as a separate manual step for the admin.
        ambassadorApplicationRepository.findByResultingAmbassadorProfileId(profile.getId())
                .filter(application -> application.getCampaign() != null)
                .ifPresent(application -> {
                    DashboardUser reviewer = application.getReviewedByUserId() != null
                            ? dashboardUserRepository.findById(application.getReviewedByUserId()).orElse(null)
                            : null;
                    campaignAssignmentService.autoAssignFromApplication(application.getCampaign(), profile, reviewer);
                });
    }

    @Transactional(readOnly = true)
    public AmbassadorPageResponse listAmbassadors(int page, int size, String sort, String search, AmbassadorStatus status) {
        Long companyId = currentUserService.getCurrentCompanyId();
        Pageable pageable = PageRequest.of(page, size, parseSort(sort));
        Page<AmbassadorProfile> result = ambassadorProfileRepository.searchByCompanyId(
                companyId,
                normalizeSearch(search),
                status,
                pageable
        );

        // Batch the per-ambassador stat counts for the whole page into 3 grouped queries instead
        // of the 3-per-row (buildStats) queries toSummary/toDetail use for a single profile - this
        // page previously fired up to size*3 sequential count queries, which was the cause of the
        // slow load reported against a company with a non-trivial number of ambassadors.
        List<Long> ambassadorUserIds = result.getContent().stream()
                .map(profile -> profile.getUser().getId())
                .toList();
        Map<Long, AmbassadorStats> statsByUserId = buildStatsMap(ambassadorUserIds, companyId);

        List<AmbassadorSummaryResponse> content = result.getContent().stream()
                .map(profile -> toSummary(profile, statsByUserId.getOrDefault(
                        profile.getUser().getId(),
                        AmbassadorStats.EMPTY
                )))
                .toList();

        return new AmbassadorPageResponse(
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
    public AmbassadorDetailResponse getAmbassador(Long ambassadorId) {
        AmbassadorProfile profile = findProfileOrThrow(ambassadorId);
        return toDetail(profile);
    }

    @Transactional
    public AmbassadorDetailResponse updateAmbassador(Long ambassadorId, UpdateAmbassadorRequest request) {
        AmbassadorProfile profile = findProfileOrThrow(ambassadorId);
        DashboardUser user = profile.getUser();

        user.setFirstName(request.firstName().trim());
        user.setLastName(request.lastName().trim());
        profile.setDisplayName(normalizeNullable(request.displayName()));
        profile.setPhone(normalizeNullable(request.phone()));
        profile.setBio(normalizeNullable(request.bio()));
        profile.setSocialMediaPlatform(normalizeNullable(request.socialMediaPlatform()));
        profile.setSocialMediaHandle(normalizeNullable(request.socialMediaHandle()));
        profile.setProfileImageUrl(normalizeNullable(request.profileImageUrl()));

        applyStatus(profile, request.status());
        return toDetail(profile);
    }

    @Transactional
    public AmbassadorDetailResponse activateAmbassador(Long ambassadorId) {
        AmbassadorProfile profile = findProfileOrThrow(ambassadorId);
        applyStatus(profile, AmbassadorStatus.ACTIVE);
        return toDetail(profile);
    }

    @Transactional
    public AmbassadorDetailResponse deactivateAmbassador(Long ambassadorId) {
        AmbassadorProfile profile = findProfileOrThrow(ambassadorId);
        applyStatus(profile, AmbassadorStatus.INACTIVE);
        disableReferralLinks(profile.getUser().getId(), profile.getCompany().getId());
        return toDetail(profile);
    }

    private Company getCurrentCompany() {
        Long companyId = currentUserService.getCurrentCompanyId();
        return companyRepository.findById(companyId)
                .orElseThrow(() -> new NotFoundException("Company not found"));
    }

    private AmbassadorProfile findProfileOrThrow(Long ambassadorId) {
        return ambassadorProfileRepository.findDetailedByIdAndCompanyId(ambassadorId, currentUserService.getCurrentCompanyId())
                .orElseThrow(() -> new NotFoundException("Ambassador not found"));
    }

    AmbassadorSummaryResponse toSummary(AmbassadorProfile profile) {
        return toSummary(profile, buildStats(profile.getUser().getId(), profile.getCompany().getId()));
    }

    private AmbassadorSummaryResponse toSummary(AmbassadorProfile profile, AmbassadorStats stats) {
        return new AmbassadorSummaryResponse(
                profile.getId(),
                profile.getUser().getId(),
                profile.getUser().getFirstName(),
                profile.getUser().getLastName(),
                profile.getUser().getUsername(),
                profile.getDisplayName(),
                profile.getStatus(),
                stats.assignedCampaigns(),
                stats.totalRegistrations(),
                stats.successfulRentals(),
                stats.conversionRate(),
                profile.getCreatedAt()
        );
    }

    private AmbassadorDetailResponse toDetail(AmbassadorProfile profile) {
        AmbassadorStats stats = buildStats(profile.getUser().getId(), profile.getCompany().getId());
        List<AmbassadorReferralLinkResponse> referralLinks = referralLinkRepository
                .findByAmbassadorUserIdAndCompanyId(profile.getUser().getId(), profile.getCompany().getId())
                .stream()
                .map(this::toReferralLinkResponse)
                .toList();

        return new AmbassadorDetailResponse(
                profile.getId(),
                profile.getUser().getId(),
                profile.getUser().getFirstName(),
                profile.getUser().getLastName(),
                profile.getUser().getUsername(),
                profile.getDisplayName(),
                profile.getPhone(),
                profile.getBio(),
                profile.getSocialMediaPlatform(),
                profile.getSocialMediaHandle(),
                profile.getProfileImageUrl(),
                profile.getAmbassadorCode(),
                profile.getStatus(),
                stats.assignedCampaigns(),
                stats.totalRegistrations(),
                stats.successfulRentals(),
                stats.conversionRate(),
                profile.getJoinedAt(),
                profile.getCreatedAt(),
                referralLinks
        );
    }

    private AmbassadorReferralLinkResponse toReferralLinkResponse(ReferralLink referralLink) {
        return new AmbassadorReferralLinkResponse(
                referralLink.getId(),
                referralLink.getCampaign().getId(),
                referralLink.getCampaign().getName(),
                referralLink.getPublicToken(),
                referralLink.getDestinationUrl(),
                referralLink.getStatus(),
                referralLink.getClickCount(),
                referralLink.getExpiresAt()
        );
    }

    private Map<Long, AmbassadorStats> buildStatsMap(List<Long> ambassadorUserIds, Long companyId) {
        if (ambassadorUserIds.isEmpty()) {
            return Map.of();
        }

        Map<Long, Long> assignedCampaignsByUserId = groupCountsByUserId(
                assignmentRepository.countByAmbassadorUserIdsAndCompanyIdAndStatusGrouped(
                        ambassadorUserIds, companyId, AssignmentStatus.ACTIVE
                )
        );
        Map<Long, Long> totalRegistrationsByUserId = groupCountsByUserId(
                referralRepository.countByAmbassadorUserIdsAndCompanyIdAndStatusInGrouped(
                        ambassadorUserIds,
                        companyId,
                        List.of(
                                ReferralStatus.REGISTERED,
                                ReferralStatus.BOOKING_STARTED,
                                ReferralStatus.BOOKING_CONFIRMED,
                                ReferralStatus.RENTAL_STARTED,
                                ReferralStatus.COMPLETED
                        )
                )
        );
        Map<Long, Long> successfulRentalsByUserId = groupCountsByUserId(
                referralRepository.countByAmbassadorUserIdsAndCompanyIdAndStatusGrouped(
                        ambassadorUserIds, companyId, ReferralStatus.COMPLETED
                )
        );

        Map<Long, AmbassadorStats> statsByUserId = new HashMap<>();
        for (Long userId : ambassadorUserIds) {
            long assignedCampaigns = assignedCampaignsByUserId.getOrDefault(userId, 0L);
            long totalRegistrations = totalRegistrationsByUserId.getOrDefault(userId, 0L);
            long successfulRentals = successfulRentalsByUserId.getOrDefault(userId, 0L);
            double conversionRate = totalRegistrations == 0
                    ? 0.0
                    : (successfulRentals * 100.0) / totalRegistrations;

            statsByUserId.put(userId, new AmbassadorStats(
                    assignedCampaigns,
                    totalRegistrations,
                    successfulRentals,
                    Math.round(conversionRate * 100.0) / 100.0
            ));
        }
        return statsByUserId;
    }

    private Map<Long, Long> groupCountsByUserId(List<Object[]> rows) {
        Map<Long, Long> counts = new HashMap<>();
        for (Object[] row : rows) {
            counts.put((Long) row[0], (Long) row[1]);
        }
        return counts;
    }

    private AmbassadorStats buildStats(Long ambassadorUserId, Long companyId) {
        long assignedCampaigns = assignmentRepository.countByAmbassadorUserIdAndCompanyIdAndStatus(
                ambassadorUserId,
                companyId,
                AssignmentStatus.ACTIVE
        );
        long totalRegistrations = referralRepository.countByAmbassadorUserIdAndCompanyIdAndStatusIn(
                ambassadorUserId,
                companyId,
                List.of(
                        ReferralStatus.REGISTERED,
                        ReferralStatus.BOOKING_STARTED,
                        ReferralStatus.BOOKING_CONFIRMED,
                        ReferralStatus.RENTAL_STARTED,
                        ReferralStatus.COMPLETED
                )
        );
        long successfulRentals = referralRepository.countByAmbassadorUserIdAndCompanyIdAndStatus(
                ambassadorUserId,
                companyId,
                ReferralStatus.COMPLETED
        );
        double conversionRate = totalRegistrations == 0
                ? 0.0
                : (successfulRentals * 100.0) / totalRegistrations;

        return new AmbassadorStats(
                assignedCampaigns,
                totalRegistrations,
                successfulRentals,
                Math.round(conversionRate * 100.0) / 100.0
        );
    }

    private void applyStatus(AmbassadorProfile profile, AmbassadorStatus status) {
        profile.setStatus(status);

        DashboardUser user = profile.getUser();
        switch (status) {
            case ACTIVE -> {
                user.setStatus(UserStatus.ACTIVE);
                if (profile.getJoinedAt() == null) {
                    profile.setJoinedAt(LocalDateTime.now());
                }
                reactivateReferralLinks(user.getId(), profile.getCompany().getId());
            }
            case INVITED -> user.setStatus(UserStatus.PENDING);
            case INACTIVE, SUSPENDED -> user.setStatus(UserStatus.INACTIVE);
        }
    }

    private void disableReferralLinks(Long ambassadorUserId, Long companyId) {
        List<ReferralLink> links = referralLinkRepository.findByAmbassadorUserIdAndCompanyIdAndStatus(
                ambassadorUserId,
                companyId,
                ReferralLinkStatus.ACTIVE
        );
        for (ReferralLink link : links) {
            link.setStatus(ReferralLinkStatus.DISABLED);
        }
    }

    private void reactivateReferralLinks(Long ambassadorUserId, Long companyId) {
        List<ReferralLink> links = referralLinkRepository.findByAmbassadorUserIdAndCompanyIdAndStatus(
                ambassadorUserId,
                companyId,
                ReferralLinkStatus.DISABLED
        );
        for (ReferralLink link : links) {
            link.setStatus(ReferralLinkStatus.ACTIVE);
        }
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
            case "firstName" -> "user.firstName";
            case "lastName" -> "user.lastName";
            case "status" -> "status";
            case "displayName" -> "displayName";
            default -> "createdAt";
        };
        Sort.Direction direction = parts.length > 1 && "asc".equalsIgnoreCase(parts[1].trim())
                ? Sort.Direction.ASC
                : Sort.Direction.DESC;

        return Sort.by(direction, property);
    }

    private String generatePlaceholderPassword() {
        return randomString(PLACEHOLDER_PASSWORD_ALPHABET, 24);
    }

    private String generateAmbassadorCode() {
        String code;
        do {
            code = "AMB-" + randomString(TOKEN_ALPHABET, 8);
        } while (ambassadorProfileRepository.existsByAmbassadorCode(code));

        return code;
    }

    private String randomString(String alphabet, int length) {
        StringBuilder builder = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            builder.append(alphabet.charAt(SECURE_RANDOM.nextInt(alphabet.length())));
        }
        return builder.toString();
    }

    private record AmbassadorStats(
            long assignedCampaigns,
            long totalRegistrations,
            long successfulRentals,
            double conversionRate
    ) {
        static final AmbassadorStats EMPTY = new AmbassadorStats(0, 0, 0, 0.0);
    }
}
