package com.actpro.referral.ambassador;

import com.actpro.referral.ambassador.dto.*;
import com.actpro.referral.auth.DashboardUser;
import com.actpro.referral.campaign.Campaign;
import com.actpro.referral.click.ReferralClick;
import com.actpro.referral.click.ReferralClickRepository;
import com.actpro.referral.common.exception.NotFoundException;
import com.actpro.referral.referral.Referral;
import com.actpro.referral.referral.ReferralLink;
import com.actpro.referral.referral.ReferralLinkRepository;
import com.actpro.referral.referral.ReferralLinkUrlService;
import com.actpro.referral.referral.ReferralStatus;
import com.actpro.referral.referral.ReferralRepository;
import com.actpro.referral.revenue.AmbassadorReward;
import com.actpro.referral.revenue.AmbassadorRewardRepository;
import com.actpro.referral.revenue.AmbassadorRewardStatus;
import com.actpro.referral.security.CurrentUserService;
import com.actpro.referral.user.PlatformUser;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
@RequiredArgsConstructor
public class AmbassadorPortalService {

    private static final EnumSet<ReferralStatus> REGISTRATION_STATUSES = EnumSet.of(
            ReferralStatus.REGISTERED,
            ReferralStatus.BOOKING_STARTED,
            ReferralStatus.BOOKING_CONFIRMED,
            ReferralStatus.RENTAL_STARTED,
            ReferralStatus.COMPLETED,
            ReferralStatus.CONVERTED
    );
    private static final EnumSet<ReferralStatus> BOOKING_STARTED_STATUSES = EnumSet.of(
            ReferralStatus.BOOKING_STARTED,
            ReferralStatus.BOOKING_CONFIRMED,
            ReferralStatus.RENTAL_STARTED,
            ReferralStatus.COMPLETED,
            ReferralStatus.CONVERTED
    );
    private static final EnumSet<ReferralStatus> COMPLETED_RENTAL_STATUSES = EnumSet.of(
            ReferralStatus.COMPLETED,
            ReferralStatus.CONVERTED
    );

    private final CurrentUserService currentUserService;
    private final AmbassadorProfileRepository ambassadorProfileRepository;
    private final CampaignAmbassadorAssignmentRepository assignmentRepository;
    private final ReferralLinkRepository referralLinkRepository;
    private final ReferralRepository referralRepository;
    private final ReferralClickRepository referralClickRepository;
    private final AmbassadorRewardRepository ambassadorRewardRepository;
    private final ReferralLinkUrlService referralLinkUrlService;

    @Transactional(readOnly = true)
    public AmbassadorDashboardResponse getDashboard() {
        AmbassadorProfile profile = getCurrentProfile();
        Long companyId = profile.getCompany().getId();
        Long ambassadorUserId = profile.getUser().getId();

        List<CampaignAmbassadorAssignment> assignments = assignmentRepository.findDetailedByAmbassadorAndStatus(
                ambassadorUserId,
                companyId,
                AssignmentStatus.ACTIVE
        );

        long totalClicks = referralClickRepository.countByAmbassadorUserIdAndCompanyId(ambassadorUserId, companyId);
        long totalRegistrations = referralRepository.countByAmbassadorUserIdAndCompanyIdAndStatusIn(
                ambassadorUserId,
                companyId,
                REGISTRATION_STATUSES
        );
        long totalBookingsStarted = referralRepository.countByAmbassadorUserIdAndCompanyIdAndStatusIn(
                ambassadorUserId,
                companyId,
                BOOKING_STARTED_STATUSES
        );
        long totalCompletedRentals = referralRepository.countByAmbassadorUserIdAndCompanyIdAndStatusIn(
                ambassadorUserId,
                companyId,
                COMPLETED_RENTAL_STATUSES
        );

        List<AmbassadorRecentReferralResponse> recentReferrals = referralRepository.findRecentByAmbassador(
                        ambassadorUserId,
                        companyId,
                        PageRequest.of(0, 5)
                ).stream()
                .map(this::toRecentReferralResponse)
                .toList();

        return new AmbassadorDashboardResponse(
                profile.getId(),
                resolveDisplayName(profile),
                assignments.size(),
                totalClicks,
                totalRegistrations,
                totalBookingsStarted,
                totalCompletedRentals,
                rate(totalRegistrations, totalClicks),
                rate(totalCompletedRentals, totalRegistrations),
                recentReferrals
        );
    }

    @Transactional(readOnly = true)
    public List<AmbassadorCampaignOverviewResponse> listCampaigns() {
        AmbassadorProfile profile = getCurrentProfile();
        Long companyId = profile.getCompany().getId();
        Long ambassadorUserId = profile.getUser().getId();

        return assignmentRepository.findDetailedByAmbassadorAndStatus(ambassadorUserId, companyId, AssignmentStatus.ACTIVE)
                .stream()
                .map(assignment -> toCampaignOverview(assignment, companyId))
                .toList();
    }

    @Transactional(readOnly = true)
    public AmbassadorCampaignDetailResponse getCampaign(Long campaignId) {
        AmbassadorProfile profile = getCurrentProfile();
        Long companyId = profile.getCompany().getId();
        Long ambassadorUserId = profile.getUser().getId();

        CampaignAmbassadorAssignment assignment = assignmentRepository
                .findDetailedByCampaignAndAmbassadorAndStatus(campaignId, ambassadorUserId, companyId, AssignmentStatus.ACTIVE)
                .orElseThrow(() -> new NotFoundException("Assigned campaign not found"));

        ReferralLink referralLink = referralLinkRepository
                .findDetailedByCampaignIdAndAmbassadorUserIdAndCompanyId(campaignId, ambassadorUserId, companyId)
                .orElseThrow(() -> new NotFoundException("Referral link not found"));

        Campaign campaign = assignment.getCampaign();
        long clickCount = referralClickRepository.countByAmbassadorUserIdAndCompanyIdAndCampaignId(ambassadorUserId, companyId, campaignId);
        long registrationCount = referralRepository.countByAmbassadorUserIdAndCompanyIdAndCampaignIdAndStatusIn(
                ambassadorUserId,
                companyId,
                campaignId,
                REGISTRATION_STATUSES
        );
        long bookingStartedCount = referralRepository.countByAmbassadorUserIdAndCompanyIdAndCampaignIdAndStatusIn(
                ambassadorUserId,
                companyId,
                campaignId,
                BOOKING_STARTED_STATUSES
        );
        long completedRentalCount = referralRepository.countByAmbassadorUserIdAndCompanyIdAndCampaignIdAndStatusIn(
                ambassadorUserId,
                companyId,
                campaignId,
                COMPLETED_RENTAL_STATUSES
        );

        return new AmbassadorCampaignDetailResponse(
                assignment.getId(),
                campaign.getId(),
                campaign.getName(),
                campaign.getDescription(),
                campaign.getLandingPageUrl(),
                campaign.getStatus(),
                campaign.getStartDate(),
                campaign.getEndDate(),
                campaign.getConversionEventName(),
                campaign.getRewardType().name(),
                campaign.getReferrerRewardValue(),
                campaign.getRefereeRewardValue(),
                clickCount,
                registrationCount,
                bookingStartedCount,
                completedRentalCount,
                rate(registrationCount, clickCount),
                rate(completedRentalCount, registrationCount),
                toReferralLinkSummary(referralLink)
        );
    }

    @Transactional(readOnly = true)
    public List<ReferralLinkSummaryResponse> listReferralLinks() {
        AmbassadorProfile profile = getCurrentProfile();
        return referralLinkRepository.findDetailedByAmbassadorUserIdAndCompanyId(profile.getUser().getId(), profile.getCompany().getId())
                .stream()
                .map(this::toReferralLinkSummary)
                .toList();
    }

    @Transactional(readOnly = true)
    public ReferralLinkSummaryResponse getCampaignReferralLink(Long campaignId) {
        AmbassadorProfile profile = getCurrentProfile();
        return referralLinkRepository
                .findDetailedByCampaignIdAndAmbassadorUserIdAndCompanyId(campaignId, profile.getUser().getId(), profile.getCompany().getId())
                .map(this::toReferralLinkSummary)
                .orElseThrow(() -> new NotFoundException("Referral link not found"));
    }

    @Transactional(readOnly = true)
    public AmbassadorReferralHistoryResponse listReferrals(
            Long campaignId,
            ReferralStatus status,
            LocalDate fromDate,
            LocalDate toDate,
            int page,
            int size
    ) {
        AmbassadorProfile profile = getCurrentProfile();
        LocalDateTime fromDateTime = fromDate != null ? fromDate.atStartOfDay() : null;
        LocalDateTime toDateTime = toDate != null ? toDate.plusDays(1).atStartOfDay().minusNanos(1) : null;

        Page<Referral> referrals = referralRepository.searchByAmbassador(
                profile.getUser().getId(),
                profile.getCompany().getId(),
                campaignId,
                status,
                fromDateTime,
                toDateTime,
                PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"))
        );

        return new AmbassadorReferralHistoryResponse(
                referrals.getContent().stream().map(this::toReferralResponse).toList(),
                referrals.getNumber(),
                referrals.getSize(),
                referrals.getTotalElements(),
                referrals.getTotalPages()
        );
    }

    @Transactional(readOnly = true)
    public AmbassadorAnalyticsResponse getAnalytics(Long campaignId, LocalDate fromDate, LocalDate toDate) {
        AmbassadorProfile profile = getCurrentProfile();
        Long companyId = profile.getCompany().getId();
        Long ambassadorUserId = profile.getUser().getId();

        LocalDate effectiveFrom = fromDate != null ? fromDate : LocalDate.now().minusDays(29);
        LocalDate effectiveTo = toDate != null ? toDate : LocalDate.now();
        LocalDateTime start = effectiveFrom.atStartOfDay();
        LocalDateTime end = effectiveTo.plusDays(1).atStartOfDay().minusNanos(1);

        List<CampaignAmbassadorAssignment> assignments = assignmentRepository.findDetailedByAmbassadorAndStatus(
                ambassadorUserId,
                companyId,
                AssignmentStatus.ACTIVE
        );
        if (campaignId != null) {
            assignments = assignments.stream()
                    .filter(assignment -> assignment.getCampaign().getId().equals(campaignId))
                    .toList();
        }

        List<ReferralClick> clicks = referralClickRepository.findByAmbassadorUserIdAndCompanyIdAndClickedAtBetween(
                ambassadorUserId,
                companyId,
                start,
                end
        );
        if (campaignId != null) {
            clicks = clicks.stream()
                    .filter(click -> click.getCampaign() != null && campaignId.equals(click.getCampaign().getId()))
                    .toList();
        }

        List<Referral> referrals = referralRepository.findByAmbassadorUserIdAndCompanyIdAndCreatedAtBetween(
                ambassadorUserId,
                companyId,
                start,
                end
        );
        if (campaignId != null) {
            referrals = referrals.stream()
                    .filter(referral -> referral.getCampaign() != null && campaignId.equals(referral.getCampaign().getId()))
                    .toList();
        }

        long totalClicks = clicks.size();
        long totalRegistrations = referrals.stream().filter(referral -> REGISTRATION_STATUSES.contains(referral.getStatus())).count();
        long totalBookingsStarted = referrals.stream().filter(referral -> BOOKING_STARTED_STATUSES.contains(referral.getStatus())).count();
        long totalCompletedRentals = referrals.stream().filter(referral -> COMPLETED_RENTAL_STATUSES.contains(referral.getStatus())).count();

        Map<Long, List<ReferralClick>> clicksByCampaign = clicks.stream()
                .filter(click -> click.getCampaign() != null)
                .collect(Collectors.groupingBy(click -> click.getCampaign().getId()));
        Map<Long, List<Referral>> referralsByCampaign = referrals.stream()
                .filter(referral -> referral.getCampaign() != null)
                .collect(Collectors.groupingBy(referral -> referral.getCampaign().getId()));

        List<AmbassadorCampaignPerformanceResponse> campaigns = assignments.stream()
                .map(assignment -> {
                    Long assignedCampaignId = assignment.getCampaign().getId();
                    List<ReferralClick> campaignClicks = clicksByCampaign.getOrDefault(assignedCampaignId, List.of());
                    List<Referral> campaignReferrals = referralsByCampaign.getOrDefault(assignedCampaignId, List.of());
                    long registrations = campaignReferrals.stream().filter(referral -> REGISTRATION_STATUSES.contains(referral.getStatus())).count();
                    long completed = campaignReferrals.stream().filter(referral -> COMPLETED_RENTAL_STATUSES.contains(referral.getStatus())).count();
                    return new AmbassadorCampaignPerformanceResponse(
                            assignedCampaignId,
                            assignment.getCampaign().getName(),
                            campaignClicks.size(),
                            registrations,
                            completed,
                            rate(registrations, campaignClicks.size()),
                            rate(completed, registrations)
                    );
                })
                .toList();

        Map<LocalDate, long[]> trendMap = new TreeMap<>();
        LocalDate cursor = effectiveFrom;
        while (!cursor.isAfter(effectiveTo)) {
            trendMap.put(cursor, new long[3]);
            cursor = cursor.plusDays(1);
        }

        for (ReferralClick click : clicks) {
            long[] point = trendMap.get(click.getClickedAt().toLocalDate());
            if (point != null) {
                point[0]++;
            }
        }
        for (Referral referral : referrals) {
            LocalDate date = referral.getCreatedAt().toLocalDate();
            long[] point = trendMap.get(date);
            if (point == null) {
                continue;
            }
            if (REGISTRATION_STATUSES.contains(referral.getStatus())) {
                point[1]++;
            }
            if (COMPLETED_RENTAL_STATUSES.contains(referral.getStatus())) {
                point[2]++;
            }
        }

        List<AmbassadorPerformanceTrendResponse> trends = trendMap.entrySet().stream()
                .map(entry -> new AmbassadorPerformanceTrendResponse(
                        entry.getKey(),
                        entry.getValue()[0],
                        entry.getValue()[1],
                        entry.getValue()[2]
                ))
                .toList();

        return new AmbassadorAnalyticsResponse(
                effectiveFrom,
                effectiveTo,
                totalClicks,
                totalRegistrations,
                totalBookingsStarted,
                totalCompletedRentals,
                rate(totalRegistrations, totalClicks),
                rate(totalCompletedRentals, totalRegistrations),
                campaigns,
                trends
        );
    }

    @Transactional(readOnly = true)
    public AmbassadorProfileResponse getProfile() {
        return toProfileResponse(getCurrentProfile());
    }

    @Transactional
    public AmbassadorProfileResponse updateProfile(UpdateAmbassadorProfileRequest request) {
        AmbassadorProfile profile = getCurrentProfile();
        DashboardUser user = profile.getUser();

        user.setFirstName(trimToNull(request.firstName()));
        user.setLastName(trimToNull(request.lastName()));
        profile.setDisplayName(trimToNull(request.displayName()));
        profile.setPhone(trimToNull(request.phone()));
        profile.setBio(trimToNull(request.bio()));
        profile.setSocialMediaPlatform(trimToNull(request.socialMediaPlatform()));
        profile.setSocialMediaHandle(trimToNull(request.socialMediaHandle()));
        profile.setProfileImageUrl(trimToNull(request.profileImageUrl()));

        return toProfileResponse(profile);
    }

    /**
     * Bounded per-ambassador aggregation in Java, not native SQL - same convention as
     * {@link #getDashboard()}/{@link #getAnalytics}, which already load a bounded list and
     * aggregate in-memory rather than reaching for {@code EntityManager} (that's reserved for
     * cross-entity/cross-tenant reporting, e.g. {@code revenue.RevenueAdminService}'s campaign report).
     */
    @Transactional(readOnly = true)
    public AmbassadorEarningsSummaryResponse getEarningsSummary() {
        AmbassadorProfile profile = getCurrentProfile();
        List<AmbassadorReward> rewards = ambassadorRewardRepository.findByAmbassadorUserIdAndCompanyId(
                profile.getUser().getId(), profile.getCompany().getId());

        BigDecimal totalPaid = sumByStatus(rewards, AmbassadorRewardStatus.PAID);
        BigDecimal totalApproved = sumByStatus(rewards, AmbassadorRewardStatus.APPROVED);
        BigDecimal totalPendingOrEligible = sumByStatuses(rewards, EnumSet.of(AmbassadorRewardStatus.PENDING, AmbassadorRewardStatus.ELIGIBLE));
        BigDecimal totalRejectedOrReversed = sumByStatuses(rewards, EnumSet.of(AmbassadorRewardStatus.REJECTED, AmbassadorRewardStatus.REVERSED));
        String currency = rewards.stream().map(AmbassadorReward::getCurrency).filter(Objects::nonNull).findFirst().orElse(null);

        return new AmbassadorEarningsSummaryResponse(totalPaid, totalApproved, totalPendingOrEligible, totalRejectedOrReversed, rewards.size(), currency);
    }

    @Transactional(readOnly = true)
    public AmbassadorEarningsHistoryResponse listEarnings(int page, int size) {
        AmbassadorProfile profile = getCurrentProfile();
        Page<AmbassadorReward> rewards = ambassadorRewardRepository.findByAmbassadorUserIdAndCompanyId(
                profile.getUser().getId(),
                profile.getCompany().getId(),
                PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"))
        );

        return new AmbassadorEarningsHistoryResponse(
                rewards.getContent().stream().map(this::toEarningResponse).toList(),
                rewards.getNumber(),
                rewards.getSize(),
                rewards.getTotalElements(),
                rewards.getTotalPages()
        );
    }

    private BigDecimal sumByStatus(List<AmbassadorReward> rewards, AmbassadorRewardStatus status) {
        return sumByStatuses(rewards, EnumSet.of(status));
    }

    private BigDecimal sumByStatuses(List<AmbassadorReward> rewards, Set<AmbassadorRewardStatus> statuses) {
        return rewards.stream()
                .filter(reward -> statuses.contains(reward.getStatus()))
                .map(AmbassadorReward::getRewardValue)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private AmbassadorEarningResponse toEarningResponse(AmbassadorReward reward) {
        return new AmbassadorEarningResponse(
                reward.getId(),
                reward.getCampaign().getId(),
                reward.getCampaign().getName(),
                reward.getReferral().getReferralCode(),
                reward.getRewardType().name(),
                reward.getRewardValue(),
                reward.getCurrency(),
                reward.getStatus(),
                reward.getHoldReason(),
                reward.getCreatedAt(),
                reward.getApprovedAt(),
                reward.getPaidAt()
        );
    }

    private AmbassadorCampaignOverviewResponse toCampaignOverview(CampaignAmbassadorAssignment assignment, Long companyId) {
        Campaign campaign = assignment.getCampaign();
        Long ambassadorUserId = assignment.getAmbassadorUser().getId();
        long clickCount = referralClickRepository.countByAmbassadorUserIdAndCompanyIdAndCampaignId(
                ambassadorUserId,
                companyId,
                campaign.getId()
        );
        long registrationCount = referralRepository.countByAmbassadorUserIdAndCompanyIdAndCampaignIdAndStatusIn(
                ambassadorUserId,
                companyId,
                campaign.getId(),
                REGISTRATION_STATUSES
        );
        long completedRentalCount = referralRepository.countByAmbassadorUserIdAndCompanyIdAndCampaignIdAndStatusIn(
                ambassadorUserId,
                companyId,
                campaign.getId(),
                COMPLETED_RENTAL_STATUSES
        );
        ReferralLink referralLink = referralLinkRepository
                .findDetailedByCampaignIdAndAmbassadorUserIdAndCompanyId(campaign.getId(), ambassadorUserId, companyId)
                .orElseThrow(() -> new NotFoundException("Referral link not found"));

        return new AmbassadorCampaignOverviewResponse(
                assignment.getId(),
                campaign.getId(),
                campaign.getName(),
                campaign.getDescription(),
                campaign.getStatus(),
                campaign.getStartDate(),
                campaign.getEndDate(),
                campaign.getConversionEventName(),
                campaign.getReferrerRewardValue(),
                campaign.getRefereeRewardValue(),
                campaign.getRewardType().name(),
                clickCount,
                registrationCount,
                completedRentalCount,
                rate(registrationCount, clickCount),
                toReferralLinkSummary(referralLink)
        );
    }

    private AmbassadorRecentReferralResponse toRecentReferralResponse(Referral referral) {
        return new AmbassadorRecentReferralResponse(
                referral.getId(),
                referral.getCampaign().getId(),
                referral.getCampaign().getName(),
                resolveCustomerName(referral),
                resolveCustomerEmail(referral),
                referral.getStatus(),
                referral.getRegisteredAt(),
                referral.getConvertedAt()
        );
    }

    private AmbassadorReferralResponse toReferralResponse(Referral referral) {
        return new AmbassadorReferralResponse(
                referral.getId(),
                referral.getCampaign().getId(),
                referral.getCampaign().getName(),
                referral.getReferralCode(),
                resolveCustomerName(referral),
                resolveCustomerEmail(referral),
                referral.getStatus(),
                referral.getCreatedAt(),
                referral.getRegisteredAt(),
                referral.getConvertedAt(),
                referral.getBookingId(),
                referral.getRentalId(),
                referral.getDiscountAmount(),
                referral.getCurrency()
        );
    }

    private AmbassadorProfileResponse toProfileResponse(AmbassadorProfile profile) {
        DashboardUser user = profile.getUser();
        return new AmbassadorProfileResponse(
                profile.getId(),
                user.getId(),
                user.getFirstName(),
                user.getLastName(),
                user.getUsername(),
                profile.getDisplayName(),
                profile.getPhone(),
                profile.getBio(),
                profile.getSocialMediaPlatform(),
                profile.getSocialMediaHandle(),
                profile.getProfileImageUrl(),
                profile.getAmbassadorCode(),
                profile.getStatus(),
                user.getStatus(),
                profile.getJoinedAt()
        );
    }

    private AmbassadorProfile getCurrentProfile() {
        return currentUserService.getCurrentAmbassadorProfile();
    }

    private ReferralLinkSummaryResponse toReferralLinkSummary(ReferralLink referralLink) {
        return new ReferralLinkSummaryResponse(
                referralLink.getId(),
                referralLink.getPublicToken(),
                referralLinkUrlService.resolveReferralUrl(referralLink),
                referralLinkUrlService.resolveQrCodeUrl(referralLink),
                referralLink.getDestinationUrl(),
                referralLink.getStatus(),
                referralLink.getClickCount(),
                referralLink.getExpiresAt()
        );
    }

    private String resolveDisplayName(AmbassadorProfile profile) {
        if (profile.getDisplayName() != null && !profile.getDisplayName().isBlank()) {
            return profile.getDisplayName();
        }
        String fullName = Stream.of(profile.getUser().getFirstName(), profile.getUser().getLastName())
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(value -> !value.isEmpty())
                .collect(Collectors.joining(" "));
        return fullName.isBlank() ? profile.getUser().getUsername() : fullName;
    }

    private String resolveCustomerName(Referral referral) {
        PlatformUser customer = referral.getCustomerUser();
        if (customer != null && customer.getName() != null && !customer.getName().isBlank()) {
            return customer.getName();
        }
        PlatformUser referrer = referral.getReferrerUser();
        if (referrer != null && referrer.getName() != null && !referrer.getName().isBlank()) {
            return referrer.getName();
        }
        return "Customer";
    }

    private String resolveCustomerEmail(Referral referral) {
        PlatformUser customer = referral.getCustomerUser();
        if (customer != null && customer.getEmail() != null && !customer.getEmail().isBlank()) {
            return maskEmail(customer.getEmail());
        }
        PlatformUser referrer = referral.getReferrerUser();
        if (referrer != null && referrer.getEmail() != null && !referrer.getEmail().isBlank()) {
            return maskEmail(referrer.getEmail());
        }
        return null;
    }

    private String maskEmail(String email) {
        int atIndex = email.indexOf('@');
        if (atIndex <= 1) {
            return email;
        }

        return email.charAt(0) + "***" + email.substring(atIndex);
    }

    private BigDecimal rate(long numerator, long denominator) {
        if (denominator <= 0) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }

        return BigDecimal.valueOf(numerator)
                .multiply(BigDecimal.valueOf(100))
                .divide(BigDecimal.valueOf(denominator), 2, RoundingMode.HALF_UP);
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
