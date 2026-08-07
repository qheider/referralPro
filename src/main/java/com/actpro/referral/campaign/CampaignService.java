package com.actpro.referral.campaign;

import com.actpro.referral.campaign.dto.CampaignResponse;
import com.actpro.referral.campaign.dto.CreateCampaignRequest;
import com.actpro.referral.campaign.dto.PublicCampaignResponse;
import com.actpro.referral.campaign.dto.UpdateCampaignRequest;
import com.actpro.referral.common.exception.BadRequestException;
import com.actpro.referral.common.exception.NotFoundException;
import com.actpro.referral.company.Company;
import com.actpro.referral.company.CompanyRepository;
import com.actpro.referral.company.CompanyStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class CampaignService {

    private final CampaignRepository campaignRepository;
    private final CompanyRepository companyRepository;
    private final CampaignCodeGenerator campaignCodeGenerator;

    @Value("${app.base-url:http://localhost:8080}")
    private String baseUrl;

    @Transactional
    public CampaignResponse createCampaign(Long companyId, CreateCampaignRequest request) {
        log.info("Creating campaign for company ID: {}", companyId);

        validateDates(request.startDate(), request.endDate(), request.ambassadorEnrollmentStart(), request.ambassadorEnrollmentEnd());

        Company company = companyRepository.findById(companyId)
                .orElseThrow(() -> new NotFoundException("Company not found"));

        Campaign campaign = new Campaign();
        campaign.setCompany(company);
        campaign.setCampaignCode(campaignCodeGenerator.generateUniqueCode());
        campaign.setName(request.name());
        campaign.setDescription(request.description());
        campaign.setLandingPageUrl(request.landingPageUrl());
        campaign.setStartDate(request.startDate());
        campaign.setEndDate(request.endDate());
        campaign.setAmbassadorEnrollmentStart(request.ambassadorEnrollmentStart());
        campaign.setAmbassadorEnrollmentEnd(request.ambassadorEnrollmentEnd());
        campaign.setRewardType(request.rewardType());
        campaign.setReferrerRewardValue(request.referrerRewardValue());
        campaign.setRefereeRewardValue(request.refereeRewardValue());
        campaign.setConversionEventName(request.conversionEventName());
        // New campaigns start DRAFT - an explicit publishCampaign() call moves them to
        // SCHEDULED/ACTIVE. Admin CRUD + lifecycle transitions, not auto-live-on-create.
        campaign.setStatus(CampaignStatus.DRAFT);

        campaign = campaignRepository.save(campaign);
        log.info("Campaign created successfully with ID: {}", campaign.getId());

        return mapToCampaignResponse(campaign);
    }

    @Transactional
    public CampaignResponse updateCampaign(Long companyId, Long campaignId, UpdateCampaignRequest request) {
        Campaign campaign = findCampaign(companyId, campaignId);
        boolean isDraft = campaign.getStatus() == CampaignStatus.DRAFT;

        if (!isDraft && isFinancialFieldChangeAttempted(campaign, request)) {
            throw new BadRequestException(
                    "Reward type, reward values, and conversion event name can only be changed while the campaign is DRAFT");
        }
        if (!isDraft && request.startDate() != null && !request.startDate().isEqual(campaign.getStartDate())) {
            throw new BadRequestException("Start date can only be changed while the campaign is DRAFT");
        }

        if (request.name() != null) {
            campaign.setName(request.name());
        }
        if (request.description() != null) {
            campaign.setDescription(request.description());
        }
        if (request.landingPageUrl() != null) {
            campaign.setLandingPageUrl(request.landingPageUrl());
        }
        if (isDraft && request.startDate() != null) {
            campaign.setStartDate(request.startDate());
        }
        if (request.endDate() != null) {
            campaign.setEndDate(request.endDate());
        }
        if (request.ambassadorEnrollmentStart() != null) {
            campaign.setAmbassadorEnrollmentStart(request.ambassadorEnrollmentStart());
        }
        if (request.ambassadorEnrollmentEnd() != null) {
            campaign.setAmbassadorEnrollmentEnd(request.ambassadorEnrollmentEnd());
        }
        if (isDraft) {
            if (request.rewardType() != null) {
                campaign.setRewardType(request.rewardType());
            }
            if (request.referrerRewardValue() != null) {
                campaign.setReferrerRewardValue(request.referrerRewardValue());
            }
            if (request.refereeRewardValue() != null) {
                campaign.setRefereeRewardValue(request.refereeRewardValue());
            }
            if (request.conversionEventName() != null) {
                campaign.setConversionEventName(request.conversionEventName());
            }
        }

        validateDates(campaign.getStartDate(), campaign.getEndDate(),
                campaign.getAmbassadorEnrollmentStart(), campaign.getAmbassadorEnrollmentEnd());

        return mapToCampaignResponse(campaign);
    }

    /**
     * DRAFT -> SCHEDULED (start date is in the future) or ACTIVE (start date has already arrived).
     */
    @Transactional
    public CampaignResponse publishCampaign(Long companyId, Long campaignId) {
        Campaign campaign = findCampaign(companyId, campaignId);
        if (campaign.getStatus() != CampaignStatus.DRAFT) {
            throw new BadRequestException("Only DRAFT campaigns can be published");
        }

        campaign.setStatus(LocalDateTime.now().isBefore(campaign.getStartDate())
                ? CampaignStatus.SCHEDULED
                : CampaignStatus.ACTIVE);

        return mapToCampaignResponse(campaign);
    }

    /**
     * ACTIVE -> PAUSED.
     */
    @Transactional
    public CampaignResponse pauseCampaign(Long companyId, Long campaignId) {
        Campaign campaign = findCampaign(companyId, campaignId);
        if (campaign.getStatus() != CampaignStatus.ACTIVE) {
            throw new BadRequestException("Only ACTIVE campaigns can be paused");
        }

        campaign.setStatus(CampaignStatus.PAUSED);
        return mapToCampaignResponse(campaign);
    }

    /**
     * PAUSED -> ACTIVE, if the referral window hasn't already ended.
     */
    @Transactional
    public CampaignResponse resumeCampaign(Long companyId, Long campaignId) {
        Campaign campaign = findCampaign(companyId, campaignId);
        if (campaign.getStatus() != CampaignStatus.PAUSED) {
            throw new BadRequestException("Only PAUSED campaigns can be resumed");
        }
        if (LocalDateTime.now().isAfter(campaign.getEndDate())) {
            throw new BadRequestException("Campaign end date has already passed; archive it instead of resuming");
        }

        campaign.setStatus(CampaignStatus.ACTIVE);
        return mapToCampaignResponse(campaign);
    }

    /**
     * SCHEDULED/ACTIVE/PAUSED -> CLOSED (admin-initiated permanent stop, as opposed to EXPIRED
     * which the worker applies automatically once the referral window ends).
     */
    @Transactional
    public CampaignResponse closeCampaign(Long companyId, Long campaignId) {
        Campaign campaign = findCampaign(companyId, campaignId);
        CampaignStatus status = campaign.getStatus();
        if (status != CampaignStatus.SCHEDULED && status != CampaignStatus.ACTIVE && status != CampaignStatus.PAUSED) {
            throw new BadRequestException("Only scheduled, active, or paused campaigns can be closed");
        }

        campaign.setStatus(CampaignStatus.CLOSED);
        return mapToCampaignResponse(campaign);
    }

    /**
     * EXPIRED/CLOSED -> ARCHIVED (terminal - historical campaigns are never physically deleted).
     */
    @Transactional
    public CampaignResponse archiveCampaign(Long companyId, Long campaignId) {
        Campaign campaign = findCampaign(companyId, campaignId);
        if (campaign.getStatus() != CampaignStatus.EXPIRED && campaign.getStatus() != CampaignStatus.CLOSED) {
            throw new BadRequestException("Only expired or closed campaigns can be archived");
        }

        campaign.setStatus(CampaignStatus.ARCHIVED);
        return mapToCampaignResponse(campaign);
    }

    /**
     * SCHEDULED -> ACTIVE for every campaign whose start date has arrived. Called by
     * CampaignExpirationWorker; each call is its own short transaction, same shape as the outbox
     * dispatcher.
     */
    @Transactional
    public int activateScheduledCampaigns() {
        List<Campaign> due = campaignRepository.findByStatusAndStartDateLessThanEqual(
                CampaignStatus.SCHEDULED, LocalDateTime.now());
        due.forEach(campaign -> campaign.setStatus(CampaignStatus.ACTIVE));
        return due.size();
    }

    /**
     * ACTIVE/PAUSED -> EXPIRED for every campaign whose referral end date has passed.
     */
    @Transactional
    public int expireDueCampaigns() {
        List<Campaign> due = campaignRepository.findByStatusInAndEndDateLessThanEqual(
                List.of(CampaignStatus.ACTIVE, CampaignStatus.PAUSED), LocalDateTime.now());
        due.forEach(campaign -> campaign.setStatus(CampaignStatus.EXPIRED));
        return due.size();
    }

    @Transactional(readOnly = true)
    public List<CampaignResponse> getCampaignsByCompany(Long companyId) {
        log.info("Fetching campaigns for company ID: {}", companyId);
        List<Campaign> campaigns = campaignRepository.findByCompanyId(companyId);
        return campaigns.stream()
                .map(this::mapToCampaignResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public CampaignResponse getCampaignById(Long companyId, Long campaignId) {
        log.info("Fetching campaign ID: {} for company ID: {}", campaignId, companyId);
        return mapToCampaignResponse(findCampaign(companyId, campaignId));
    }

    /**
     * Public, unauthenticated resolution of a published campaign's join link - see
     * PublicCampaignController. A DRAFT campaign's code is treated identically to an unknown code
     * so its existence isn't leaked before the admin publishes it.
     */
    @Transactional(readOnly = true)
    public PublicCampaignResponse resolveJoinLink(String campaignCode) {
        Campaign campaign = campaignRepository.findByCampaignCode(campaignCode)
                .orElseThrow(() -> new NotFoundException("Campaign not found"));

        if (campaign.getStatus() == CampaignStatus.DRAFT) {
            throw new NotFoundException("Campaign not found");
        }

        Company company = campaign.getCompany();
        boolean companyActive = company.getStatus() == CompanyStatus.ACTIVE;
        boolean enrollmentOpen = companyActive && campaign.isEnrollmentOpen();
        String reason = enrollmentOpen ? null : unavailableReason(campaign, companyActive);

        return new PublicCampaignResponse(
                campaign.getCampaignCode(),
                company.getId(),
                company.getName(),
                campaign.getName(),
                campaign.getDescription(),
                enrollmentOpen,
                reason
        );
    }

    private String unavailableReason(Campaign campaign, boolean companyActive) {
        if (!companyActive) {
            return "This company is not currently accepting ambassadors.";
        }
        return switch (campaign.getStatus()) {
            case PAUSED -> "Ambassador enrollment is currently paused.";
            case EXPIRED, CLOSED, ARCHIVED -> "This campaign is no longer accepting ambassadors.";
            // SCHEDULED and ACTIVE both allow an open enrollment window (see
            // Campaign.isEnrollmentOpen) - if we got here enrollmentOpen was false, so it's a
            // window-timing reason either way, not the campaign's referral-period status.
            case SCHEDULED, ACTIVE -> LocalDateTime.now().isBefore(campaign.getAmbassadorEnrollmentStart())
                    ? "Ambassador enrollment has not opened yet."
                    : "Ambassador enrollment has closed.";
            case DRAFT -> "This campaign is not yet published.";
        };
    }

    private boolean isFinancialFieldChangeAttempted(Campaign campaign, UpdateCampaignRequest request) {
        return (request.rewardType() != null && request.rewardType() != campaign.getRewardType())
                || (request.referrerRewardValue() != null && request.referrerRewardValue().compareTo(campaign.getReferrerRewardValue()) != 0)
                || (request.refereeRewardValue() != null && request.refereeRewardValue().compareTo(campaign.getRefereeRewardValue()) != 0)
                || (request.conversionEventName() != null && !request.conversionEventName().equals(campaign.getConversionEventName()));
    }

    private void validateDates(LocalDateTime startDate, LocalDateTime endDate,
                                LocalDateTime enrollmentStart, LocalDateTime enrollmentEnd) {
        if (endDate.isBefore(startDate)) {
            throw new BadRequestException("Campaign end date must be after start date");
        }
        if (enrollmentEnd.isBefore(enrollmentStart)) {
            throw new BadRequestException("Ambassador enrollment end date must be after its start date");
        }
        if (enrollmentEnd.isAfter(endDate)) {
            throw new BadRequestException("Ambassador enrollment must end on or before the campaign's referral end date");
        }
    }

    private Campaign findCampaign(Long companyId, Long campaignId) {
        return campaignRepository.findByIdAndCompanyId(campaignId, companyId)
                .orElseThrow(() -> new NotFoundException("Campaign not found"));
    }

    private CampaignResponse mapToCampaignResponse(Campaign campaign) {
        return new CampaignResponse(
                campaign.getId(),
                campaign.getCampaignCode(),
                baseUrl + "/join/" + campaign.getCampaignCode(),
                campaign.getName(),
                campaign.getDescription(),
                campaign.getLandingPageUrl(),
                campaign.getStartDate(),
                campaign.getEndDate(),
                campaign.getAmbassadorEnrollmentStart(),
                campaign.getAmbassadorEnrollmentEnd(),
                campaign.getRewardType(),
                campaign.getReferrerRewardValue(),
                campaign.getRefereeRewardValue(),
                campaign.getConversionEventName(),
                campaign.getStatus(),
                campaign.getCreatedAt()
        );
    }
}
