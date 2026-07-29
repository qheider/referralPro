package com.actpro.referral.ambassador;

import com.actpro.referral.ambassador.dto.AssignAmbassadorsRequest;
import com.actpro.referral.ambassador.dto.AssignedCampaignResponse;
import com.actpro.referral.ambassador.dto.CampaignAssignmentResponse;
import com.actpro.referral.ambassador.dto.ReferralLinkSummaryResponse;
import com.actpro.referral.auth.DashboardUser;
import com.actpro.referral.auth.UserRole;
import com.actpro.referral.auth.UserStatus;
import com.actpro.referral.campaign.Campaign;
import com.actpro.referral.campaign.CampaignRepository;
import com.actpro.referral.common.exception.BadRequestException;
import com.actpro.referral.common.exception.NotFoundException;
import com.actpro.referral.referral.ReferralLink;
import com.actpro.referral.referral.ReferralLinkRepository;
import com.actpro.referral.referral.ReferralLinkStatus;
import com.actpro.referral.referral.ReferralTokenGenerator;
import com.actpro.referral.security.CurrentUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class CampaignAssignmentService {

    private final CampaignRepository campaignRepository;
    private final AmbassadorProfileRepository ambassadorProfileRepository;
    private final CampaignAmbassadorAssignmentRepository assignmentRepository;
    private final ReferralLinkRepository referralLinkRepository;
    private final ReferralTokenGenerator referralTokenGenerator;
    private final CurrentUserService currentUserService;

    @Value("${app.base-url:http://localhost:8080}")
    private String baseUrl;

    @Transactional
    public List<CampaignAssignmentResponse> assignAmbassadors(Long campaignId, AssignAmbassadorsRequest request) {
        Long companyId = currentUserService.getCurrentCompanyId();
        Campaign campaign = findCampaign(companyId, campaignId);
        DashboardUser assignedBy = currentUserService.getCurrentUser();

        Set<Long> ambassadorIds = new LinkedHashSet<>(request.ambassadorIds());
        if (ambassadorIds.isEmpty()) {
            throw new BadRequestException("At least one ambassador is required");
        }

        List<CampaignAssignmentResponse> responses = new ArrayList<>();
        for (Long ambassadorId : ambassadorIds) {
            AmbassadorProfile profile = ambassadorProfileRepository.findDetailedByIdAndCompanyId(ambassadorId, companyId)
                    .orElseThrow(() -> new NotFoundException("Ambassador not found"));

            validateAmbassador(profile);

            CampaignAmbassadorAssignment assignment = assignmentRepository
                    .findByCampaignIdAndAmbassadorUserIdAndCompanyId(campaign.getId(), profile.getUser().getId(), companyId)
                    .orElseGet(CampaignAmbassadorAssignment::new);

            if (assignment.getId() != null && assignment.getStatus() == AssignmentStatus.ACTIVE) {
                throw new BadRequestException("Ambassador is already assigned to this campaign");
            }

            assignment.setCompany(campaign.getCompany());
            assignment.setCampaign(campaign);
            assignment.setAmbassadorUser(profile.getUser());
            assignment.setAssignedBy(assignedBy);
            assignment.setAssignedAt(java.time.LocalDateTime.now());
            assignment.setStatus(AssignmentStatus.ACTIVE);
            assignment = assignmentRepository.save(assignment);

            ReferralLink referralLink = referralLinkRepository
                    .findByCampaignIdAndAmbassadorUserIdAndCompanyId(campaign.getId(), profile.getUser().getId(), companyId)
                    .orElseGet(ReferralLink::new);

            if (referralLink.getId() == null) {
                referralLink.setPublicToken(referralTokenGenerator.generateUniqueToken());
                referralLink.setClickCount(0L);
            }

            referralLink.setCompany(campaign.getCompany());
            referralLink.setCampaign(campaign);
            referralLink.setAmbassadorUser(profile.getUser());
            referralLink.setAssignment(assignment);
            referralLink.setDestinationUrl(campaign.getLandingPageUrl());
            referralLink.setStatus(ReferralLinkStatus.ACTIVE);
            referralLink = referralLinkRepository.save(referralLink);

            responses.add(toAssignmentResponse(assignment, profile, referralLink));
        }

        return responses;
    }

    @Transactional(readOnly = true)
    public List<CampaignAssignmentResponse> listCampaignAssignments(Long campaignId) {
        Long companyId = currentUserService.getCurrentCompanyId();
        findCampaign(companyId, campaignId);

        return assignmentRepository.findDetailedByCampaignAndStatus(campaignId, companyId, AssignmentStatus.ACTIVE)
                .stream()
                .map(assignment -> {
                    AmbassadorProfile profile = ambassadorProfileRepository
                            .findByCompanyIdAndUserId(companyId, assignment.getAmbassadorUser().getId())
                            .orElseThrow(() -> new NotFoundException("Ambassador not found"));
                    ReferralLink referralLink = referralLinkRepository
                            .findByCampaignIdAndAmbassadorUserIdAndCompanyId(campaignId, assignment.getAmbassadorUser().getId(), companyId)
                            .orElseThrow(() -> new NotFoundException("Referral link not found"));
                    return toAssignmentResponse(assignment, profile, referralLink);
                })
                .toList();
    }

    @Transactional
    public void removeCampaignAssignment(Long campaignId, Long ambassadorId) {
        Long companyId = currentUserService.getCurrentCompanyId();
        findCampaign(companyId, campaignId);

        AmbassadorProfile profile = ambassadorProfileRepository.findDetailedByIdAndCompanyId(ambassadorId, companyId)
                .orElseThrow(() -> new NotFoundException("Ambassador not found"));

        CampaignAmbassadorAssignment assignment = assignmentRepository
                .findByCampaignIdAndAmbassadorUserIdAndCompanyId(campaignId, profile.getUser().getId(), companyId)
                .orElseThrow(() -> new NotFoundException("Campaign assignment not found"));

        assignment.setStatus(AssignmentStatus.REMOVED);

        ReferralLink referralLink = referralLinkRepository
                .findByCampaignIdAndAmbassadorUserIdAndCompanyId(campaignId, profile.getUser().getId(), companyId)
                .orElseThrow(() -> new NotFoundException("Referral link not found"));
        referralLink.setStatus(ReferralLinkStatus.DISABLED);
    }

    @Transactional(readOnly = true)
    public List<AssignedCampaignResponse> listAmbassadorCampaigns(Long ambassadorId) {
        Long companyId = currentUserService.getCurrentCompanyId();
        AmbassadorProfile profile = ambassadorProfileRepository.findDetailedByIdAndCompanyId(ambassadorId, companyId)
                .orElseThrow(() -> new NotFoundException("Ambassador not found"));

        return assignmentRepository.findDetailedByAmbassadorAndStatus(profile.getUser().getId(), companyId, AssignmentStatus.ACTIVE)
                .stream()
                .map(assignment -> {
                    ReferralLink referralLink = referralLinkRepository
                            .findByCampaignIdAndAmbassadorUserIdAndCompanyId(
                                    assignment.getCampaign().getId(),
                                    assignment.getAmbassadorUser().getId(),
                                    companyId
                            )
                            .orElseThrow(() -> new NotFoundException("Referral link not found"));

                    return new AssignedCampaignResponse(
                            assignment.getId(),
                            assignment.getCampaign().getId(),
                            assignment.getCampaign().getName(),
                            assignment.getCampaign().getStatus(),
                            assignment.getCampaign().getStartDate(),
                            assignment.getCampaign().getEndDate(),
                            assignment.getStatus(),
                            assignment.getAssignedAt(),
                            toReferralLinkSummary(referralLink)
                    );
                })
                .toList();
    }

    private Campaign findCampaign(Long companyId, Long campaignId) {
        return campaignRepository.findByIdAndCompanyId(campaignId, companyId)
                .orElseThrow(() -> new NotFoundException("Campaign not found"));
    }

    private void validateAmbassador(AmbassadorProfile profile) {
        DashboardUser user = profile.getUser();
        if (user.getRole() != UserRole.AMBASSADOR) {
            throw new BadRequestException("User is not an ambassador");
        }

        if (user.getStatus() != UserStatus.ACTIVE || profile.getStatus() != AmbassadorStatus.ACTIVE) {
            throw new BadRequestException("Ambassador must be active before campaign assignment");
        }
    }

    private CampaignAssignmentResponse toAssignmentResponse(
            CampaignAmbassadorAssignment assignment,
            AmbassadorProfile profile,
            ReferralLink referralLink
    ) {
        DashboardUser ambassador = assignment.getAmbassadorUser();
        return new CampaignAssignmentResponse(
                assignment.getId(),
                assignment.getCampaign().getId(),
                profile.getId(),
                ambassador.getId(),
                ambassador.getFirstName(),
                ambassador.getLastName(),
                ambassador.getUsername(),
                profile.getDisplayName(),
                assignment.getStatus(),
                assignment.getAssignedAt(),
                toReferralLinkSummary(referralLink)
        );
    }

    private ReferralLinkSummaryResponse toReferralLinkSummary(ReferralLink referralLink) {
        return new ReferralLinkSummaryResponse(
                referralLink.getId(),
                referralLink.getPublicToken(),
                baseUrl + "/r/" + referralLink.getPublicToken(),
                referralLink.getDestinationUrl(),
                referralLink.getStatus(),
                referralLink.getClickCount(),
                referralLink.getExpiresAt()
        );
    }
}
