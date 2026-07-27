package com.actpro.referral.ambassador;

import com.actpro.referral.ambassador.dto.*;
import com.actpro.referral.auth.DashboardUser;
import com.actpro.referral.auth.DashboardUserRepository;
import com.actpro.referral.auth.UserRole;
import com.actpro.referral.auth.UserStatus;
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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class AmbassadorAdminService {

    private static final String TOKEN_ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
    private static final String PASSWORD_ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz23456789!@#$%";
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final AmbassadorProfileRepository ambassadorProfileRepository;
    private final CampaignAmbassadorAssignmentRepository assignmentRepository;
    private final DashboardUserRepository dashboardUserRepository;
    private final CompanyRepository companyRepository;
    private final ReferralRepository referralRepository;
    private final ReferralLinkRepository referralLinkRepository;
    private final PasswordEncoder passwordEncoder;
    private final CurrentUserService currentUserService;

    @Transactional
    public AmbassadorSummaryResponse createAmbassador(CreateAmbassadorRequest request) {
        Company company = getCurrentCompany();

        if (company.getStatus() != CompanyStatus.ACTIVE) {
            throw new BadRequestException("Company must be active to create ambassadors");
        }

        if (dashboardUserRepository.existsByUsername(request.email())) {
            throw new BadRequestException("Email is already in use");
        }

        DashboardUser user = new DashboardUser();
        user.setCompany(company);
        user.setUsername(request.email().trim().toLowerCase(Locale.ROOT));
        user.setPassword(passwordEncoder.encode(generateTemporaryPassword()));
        user.setFirstName(request.firstName().trim());
        user.setLastName(request.lastName().trim());
        user.setRole(UserRole.AMBASSADOR);
        user.setStatus(UserStatus.PENDING);
        user = dashboardUserRepository.save(user);

        AmbassadorProfile profile = new AmbassadorProfile();
        profile.setUser(user);
        profile.setCompany(company);
        profile.setDisplayName(normalizeNullable(request.displayName()));
        profile.setPhone(normalizeNullable(request.phone()));
        profile.setSocialMediaPlatform(normalizeNullable(request.socialMediaPlatform()));
        profile.setSocialMediaHandle(normalizeNullable(request.socialMediaHandle()));
        profile.setAmbassadorCode(generateAmbassadorCode());
        profile.setStatus(AmbassadorStatus.INVITED);
        profile = ambassadorProfileRepository.save(profile);

        return toSummary(profile);
    }

    @Transactional(readOnly = true)
    public AmbassadorPageResponse listAmbassadors(int page, int size, String sort, String search, AmbassadorStatus status) {
        Pageable pageable = PageRequest.of(page, size, parseSort(sort));
        Page<AmbassadorProfile> result = ambassadorProfileRepository.searchByCompanyId(
                currentUserService.getCurrentCompanyId(),
                normalizeSearch(search),
                status,
                pageable
        );

        List<AmbassadorSummaryResponse> content = result.getContent().stream()
                .map(this::toSummary)
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

    private AmbassadorSummaryResponse toSummary(AmbassadorProfile profile) {
        AmbassadorStats stats = buildStats(profile.getUser().getId(), profile.getCompany().getId());
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

    private String generateTemporaryPassword() {
        return randomString(PASSWORD_ALPHABET, 24);
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
    }
}
