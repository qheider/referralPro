package com.actpro.referral.campaign;

import com.actpro.referral.campaign.dto.CampaignResponse;
import com.actpro.referral.campaign.dto.CreateCampaignRequest;
import com.actpro.referral.common.exception.BadRequestException;
import com.actpro.referral.common.exception.NotFoundException;
import com.actpro.referral.company.Company;
import com.actpro.referral.company.CompanyRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class CampaignService {

    private final CampaignRepository campaignRepository;
    private final CompanyRepository companyRepository;

    @Transactional
    public CampaignResponse createCampaign(Long companyId, CreateCampaignRequest request) {
        log.info("Creating campaign for company ID: {}", companyId);

        // Validate end date is after start date
        if (request.endDate().isBefore(request.startDate())) {
            throw new BadRequestException("Campaign end date must be after start date");
        }

        // Find company
        Company company = companyRepository.findById(companyId)
                .orElseThrow(() -> new NotFoundException("Company not found"));

        // Create campaign
        Campaign campaign = new Campaign();
        campaign.setCompany(company);
        campaign.setName(request.name());
        campaign.setDescription(request.description());
        campaign.setLandingPageUrl(request.landingPageUrl());
        campaign.setStartDate(request.startDate());
        campaign.setEndDate(request.endDate());
        campaign.setRewardType(request.rewardType());
        campaign.setReferrerRewardValue(request.referrerRewardValue());
        campaign.setRefereeRewardValue(request.refereeRewardValue());
        campaign.setConversionEventName(request.conversionEventName());
        campaign.setStatus(CampaignStatus.ACTIVE);

        campaign = campaignRepository.save(campaign);
        log.info("Campaign created successfully with ID: {}", campaign.getId());

        return mapToCampaignResponse(campaign);
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
        Campaign campaign = campaignRepository.findByIdAndCompanyId(campaignId, companyId)
                .orElseThrow(() -> new NotFoundException("Campaign not found"));
        return mapToCampaignResponse(campaign);
    }

    private CampaignResponse mapToCampaignResponse(Campaign campaign) {
        return new CampaignResponse(
                campaign.getId(),
                campaign.getName(),
                campaign.getDescription(),
                campaign.getLandingPageUrl(),
                campaign.getStartDate(),
                campaign.getEndDate(),
                campaign.getRewardType(),
                campaign.getReferrerRewardValue(),
                campaign.getRefereeRewardValue(),
                campaign.getConversionEventName(),
                campaign.getStatus(),
                campaign.getCreatedAt()
        );
    }
}
