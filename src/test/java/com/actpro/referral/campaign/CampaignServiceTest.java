package com.actpro.referral.campaign;

import com.actpro.referral.ambassador.AssignmentStatus;
import com.actpro.referral.campaign.dto.CampaignResponse;
import com.actpro.referral.campaign.dto.CreateCampaignRequest;
import com.actpro.referral.campaign.dto.PublicCampaignResponse;
import com.actpro.referral.campaign.dto.UpdateCampaignRequest;
import com.actpro.referral.common.exception.BadRequestException;
import com.actpro.referral.common.exception.NotFoundException;
import com.actpro.referral.company.Company;
import com.actpro.referral.company.CompanyRepository;
import com.actpro.referral.company.CompanyStatus;
import com.actpro.referral.referral.ReferralLink;
import com.actpro.referral.referral.ReferralLinkRepository;
import com.actpro.referral.referral.ReferralLinkStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CampaignServiceTest {

    @Mock
    private CampaignRepository campaignRepository;

    @Mock
    private CompanyRepository companyRepository;

    @Mock
    private CampaignCodeGenerator campaignCodeGenerator;

    @Mock
    private ReferralLinkRepository referralLinkRepository;

    @InjectMocks
    private CampaignService campaignService;

    private Company company;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(campaignService, "frontendUrl", "https://app.referralpro.com");

        company = new Company();
        company.setId(5L);
        company.setName("Acme");
        company.setStatus(CompanyStatus.ACTIVE);
    }

    // --- createCampaign -----------------------------------------------------------------

    @Test
    void shouldCreateCampaignAsDraftWithGeneratedCode() {
        when(companyRepository.findById(5L)).thenReturn(Optional.of(company));
        when(campaignCodeGenerator.generateUniqueCode()).thenReturn("ABCD1234EF");
        when(campaignRepository.save(any(Campaign.class))).thenAnswer(invocation -> {
            Campaign campaign = invocation.getArgument(0);
            campaign.setId(10L);
            return campaign;
        });

        CampaignResponse response = campaignService.createCampaign(5L, validCreateRequest());

        assertEquals(CampaignStatus.DRAFT, response.status());
        assertEquals("ABCD1234EF", response.campaignCode());
        assertEquals("https://app.referralpro.com/join/ABCD1234EF", response.joinLink());

        ArgumentCaptor<Campaign> captor = ArgumentCaptor.forClass(Campaign.class);
        org.mockito.Mockito.verify(campaignRepository).save(captor.capture());
        assertEquals(CampaignStatus.DRAFT, captor.getValue().getStatus());
        assertEquals(company, captor.getValue().getCompany());
    }

    @Test
    void shouldRejectCreateWhenReferralEndBeforeStart() {
        CreateCampaignRequest request = createRequestWithDates(
                LocalDateTime.now().plusDays(10), LocalDateTime.now().plusDays(1),
                LocalDateTime.now(), LocalDateTime.now().plusDays(1));

        assertThrows(BadRequestException.class, () -> campaignService.createCampaign(5L, request));
    }

    @Test
    void shouldRejectCreateWhenEnrollmentEndBeforeEnrollmentStart() {
        CreateCampaignRequest request = createRequestWithDates(
                LocalDateTime.now(), LocalDateTime.now().plusDays(30),
                LocalDateTime.now().plusDays(5), LocalDateTime.now().plusDays(1));

        assertThrows(BadRequestException.class, () -> campaignService.createCampaign(5L, request));
    }

    @Test
    void shouldRejectCreateWhenEnrollmentEndAfterReferralEnd() {
        CreateCampaignRequest request = createRequestWithDates(
                LocalDateTime.now(), LocalDateTime.now().plusDays(10),
                LocalDateTime.now(), LocalDateTime.now().plusDays(20));

        assertThrows(BadRequestException.class, () -> campaignService.createCampaign(5L, request));
    }

    @Test
    void shouldRejectCreateWhenCompanyNotFound() {
        when(companyRepository.findById(5L)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> campaignService.createCampaign(5L, validCreateRequest()));
    }

    // --- updateCampaign -------------------------------------------------------------------

    @Test
    void shouldAllowFinancialFieldChangesWhileDraft() {
        Campaign campaign = draftCampaign();
        when(campaignRepository.findByIdAndCompanyId(10L, 5L)).thenReturn(Optional.of(campaign));

        UpdateCampaignRequest request = new UpdateCampaignRequest(
                null, null, null, null, null, null, null, null, null, null, null,
                RewardType.CREDIT, BigDecimal.TEN, BigDecimal.ONE, "signup_completed");

        CampaignResponse response = campaignService.updateCampaign(5L, 10L, request);

        assertEquals(RewardType.CREDIT, response.rewardType());
        assertEquals(0, BigDecimal.TEN.compareTo(response.referrerRewardValue()));
        assertEquals("signup_completed", response.conversionEventName());
    }

    @Test
    void shouldRejectFinancialFieldChangeOncePublished() {
        Campaign campaign = draftCampaign();
        campaign.setStatus(CampaignStatus.ACTIVE);
        when(campaignRepository.findByIdAndCompanyId(10L, 5L)).thenReturn(Optional.of(campaign));

        UpdateCampaignRequest request = new UpdateCampaignRequest(
                null, null, null, null, null, null, null, null, null, null, null,
                RewardType.CREDIT, null, null, null);

        assertThrows(BadRequestException.class, () -> campaignService.updateCampaign(5L, 10L, request));
    }

    @Test
    void shouldRejectStartDateChangeOncePublished() {
        Campaign campaign = draftCampaign();
        campaign.setStatus(CampaignStatus.ACTIVE);
        when(campaignRepository.findByIdAndCompanyId(10L, 5L)).thenReturn(Optional.of(campaign));

        UpdateCampaignRequest request = new UpdateCampaignRequest(
                null, null, null, null, null, null, null, campaign.getStartDate().plusDays(1), null, null, null,
                null, null, null, null);

        assertThrows(BadRequestException.class, () -> campaignService.updateCampaign(5L, 10L, request));
    }

    @Test
    void shouldAllowNameAndDescriptionChangeAfterPublish() {
        Campaign campaign = draftCampaign();
        campaign.setStatus(CampaignStatus.ACTIVE);
        when(campaignRepository.findByIdAndCompanyId(10L, 5L)).thenReturn(Optional.of(campaign));

        UpdateCampaignRequest request = new UpdateCampaignRequest(
                "New name", "New description", null, null, null, null, null, null, null, null, null,
                null, null, null, null);

        CampaignResponse response = campaignService.updateCampaign(5L, 10L, request);

        assertEquals("New name", response.name());
        assertEquals("New description", response.description());
    }

    // --- lifecycle transitions --------------------------------------------------------------

    @Test
    void shouldPublishDraftToScheduledWhenStartDateInFuture() {
        Campaign campaign = draftCampaign();
        campaign.setStartDate(LocalDateTime.now().plusDays(5));
        when(campaignRepository.findByIdAndCompanyId(10L, 5L)).thenReturn(Optional.of(campaign));

        CampaignResponse response = campaignService.publishCampaign(5L, 10L);

        assertEquals(CampaignStatus.SCHEDULED, response.status());
    }

    @Test
    void shouldPublishDraftToActiveWhenStartDateAlreadyArrived() {
        Campaign campaign = draftCampaign();
        campaign.setStartDate(LocalDateTime.now().minusDays(1));
        when(campaignRepository.findByIdAndCompanyId(10L, 5L)).thenReturn(Optional.of(campaign));

        CampaignResponse response = campaignService.publishCampaign(5L, 10L);

        assertEquals(CampaignStatus.ACTIVE, response.status());
    }

    @Test
    void shouldRejectPublishingNonDraftCampaign() {
        Campaign campaign = draftCampaign();
        campaign.setStatus(CampaignStatus.ACTIVE);
        when(campaignRepository.findByIdAndCompanyId(10L, 5L)).thenReturn(Optional.of(campaign));

        assertThrows(BadRequestException.class, () -> campaignService.publishCampaign(5L, 10L));
    }

    @Test
    void shouldPauseActiveCampaign() {
        Campaign campaign = draftCampaign();
        campaign.setStatus(CampaignStatus.ACTIVE);
        when(campaignRepository.findByIdAndCompanyId(10L, 5L)).thenReturn(Optional.of(campaign));

        CampaignResponse response = campaignService.pauseCampaign(5L, 10L);

        assertEquals(CampaignStatus.PAUSED, response.status());
    }

    @Test
    void shouldDisableActiveReferralLinksWhenPausing() {
        Campaign campaign = draftCampaign();
        campaign.setStatus(CampaignStatus.ACTIVE);
        when(campaignRepository.findByIdAndCompanyId(10L, 5L)).thenReturn(Optional.of(campaign));

        ReferralLink link = new ReferralLink();
        link.setStatus(ReferralLinkStatus.ACTIVE);
        when(referralLinkRepository.findByCampaignIdAndStatusAndAssignment_Status(
                10L, ReferralLinkStatus.ACTIVE, AssignmentStatus.ACTIVE))
                .thenReturn(List.of(link));

        campaignService.pauseCampaign(5L, 10L);

        assertEquals(ReferralLinkStatus.DISABLED, link.getStatus());
    }

    @Test
    void shouldRejectPausingNonActiveCampaign() {
        Campaign campaign = draftCampaign();
        when(campaignRepository.findByIdAndCompanyId(10L, 5L)).thenReturn(Optional.of(campaign));

        assertThrows(BadRequestException.class, () -> campaignService.pauseCampaign(5L, 10L));
    }

    @Test
    void shouldResumePausedCampaignBeforeEndDate() {
        Campaign campaign = draftCampaign();
        campaign.setStatus(CampaignStatus.PAUSED);
        campaign.setEndDate(LocalDateTime.now().plusDays(5));
        when(campaignRepository.findByIdAndCompanyId(10L, 5L)).thenReturn(Optional.of(campaign));

        CampaignResponse response = campaignService.resumeCampaign(5L, 10L);

        assertEquals(CampaignStatus.ACTIVE, response.status());
    }

    @Test
    void shouldReactivateDisabledReferralLinksWhenResuming() {
        Campaign campaign = draftCampaign();
        campaign.setStatus(CampaignStatus.PAUSED);
        campaign.setEndDate(LocalDateTime.now().plusDays(5));
        when(campaignRepository.findByIdAndCompanyId(10L, 5L)).thenReturn(Optional.of(campaign));

        ReferralLink link = new ReferralLink();
        link.setStatus(ReferralLinkStatus.DISABLED);
        when(referralLinkRepository.findByCampaignIdAndStatusAndAssignment_Status(
                10L, ReferralLinkStatus.DISABLED, AssignmentStatus.ACTIVE))
                .thenReturn(List.of(link));

        campaignService.resumeCampaign(5L, 10L);

        assertEquals(ReferralLinkStatus.ACTIVE, link.getStatus());
        // Only links whose assignment is still ACTIVE are ever queried - a link disabled because
        // its ambassador was individually removed from the campaign must never come back via a
        // campaign-level resume.
        verify(referralLinkRepository).findByCampaignIdAndStatusAndAssignment_Status(
                10L, ReferralLinkStatus.DISABLED, AssignmentStatus.ACTIVE);
        verify(referralLinkRepository, never()).findByCampaignIdAndStatusAndAssignment_Status(
                10L, ReferralLinkStatus.DISABLED, AssignmentStatus.REMOVED);
    }

    @Test
    void shouldRejectResumingPastEndDate() {
        Campaign campaign = draftCampaign();
        campaign.setStatus(CampaignStatus.PAUSED);
        campaign.setEndDate(LocalDateTime.now().minusDays(1));
        when(campaignRepository.findByIdAndCompanyId(10L, 5L)).thenReturn(Optional.of(campaign));

        assertThrows(BadRequestException.class, () -> campaignService.resumeCampaign(5L, 10L));
    }

    @Test
    void shouldCloseActiveCampaign() {
        Campaign campaign = draftCampaign();
        campaign.setStatus(CampaignStatus.ACTIVE);
        when(campaignRepository.findByIdAndCompanyId(10L, 5L)).thenReturn(Optional.of(campaign));

        CampaignResponse response = campaignService.closeCampaign(5L, 10L);

        assertEquals(CampaignStatus.CLOSED, response.status());
    }

    @Test
    void shouldDisableActiveReferralLinksWhenClosing() {
        Campaign campaign = draftCampaign();
        campaign.setStatus(CampaignStatus.ACTIVE);
        when(campaignRepository.findByIdAndCompanyId(10L, 5L)).thenReturn(Optional.of(campaign));

        ReferralLink link = new ReferralLink();
        link.setStatus(ReferralLinkStatus.ACTIVE);
        when(referralLinkRepository.findByCampaignIdAndStatusAndAssignment_Status(
                10L, ReferralLinkStatus.ACTIVE, AssignmentStatus.ACTIVE))
                .thenReturn(List.of(link));

        campaignService.closeCampaign(5L, 10L);

        assertEquals(ReferralLinkStatus.DISABLED, link.getStatus());
    }

    @Test
    void shouldRejectClosingDraftCampaign() {
        Campaign campaign = draftCampaign();
        when(campaignRepository.findByIdAndCompanyId(10L, 5L)).thenReturn(Optional.of(campaign));

        assertThrows(BadRequestException.class, () -> campaignService.closeCampaign(5L, 10L));
    }

    @Test
    void shouldArchiveClosedCampaign() {
        Campaign campaign = draftCampaign();
        campaign.setStatus(CampaignStatus.CLOSED);
        when(campaignRepository.findByIdAndCompanyId(10L, 5L)).thenReturn(Optional.of(campaign));

        CampaignResponse response = campaignService.archiveCampaign(5L, 10L);

        assertEquals(CampaignStatus.ARCHIVED, response.status());
    }

    @Test
    void shouldRejectArchivingActiveCampaign() {
        Campaign campaign = draftCampaign();
        campaign.setStatus(CampaignStatus.ACTIVE);
        when(campaignRepository.findByIdAndCompanyId(10L, 5L)).thenReturn(Optional.of(campaign));

        assertThrows(BadRequestException.class, () -> campaignService.archiveCampaign(5L, 10L));
    }

    // --- expiration worker methods ----------------------------------------------------------

    @Test
    void shouldActivateDueScheduledCampaigns() {
        Campaign campaign = draftCampaign();
        campaign.setStatus(CampaignStatus.SCHEDULED);
        when(campaignRepository.findByStatusAndStartDateLessThanEqual(any(), any()))
                .thenReturn(List.of(campaign));

        int count = campaignService.activateScheduledCampaigns();

        assertEquals(1, count);
        assertEquals(CampaignStatus.ACTIVE, campaign.getStatus());
    }

    @Test
    void shouldExpireDueActiveAndPausedCampaigns() {
        Campaign campaign = draftCampaign();
        campaign.setStatus(CampaignStatus.ACTIVE);
        when(campaignRepository.findByStatusInAndEndDateLessThanEqual(any(), any()))
                .thenReturn(List.of(campaign));

        int count = campaignService.expireDueCampaigns();

        assertEquals(1, count);
        assertEquals(CampaignStatus.EXPIRED, campaign.getStatus());
    }

    @Test
    void shouldDisableActiveReferralLinksWhenExpiring() {
        Campaign campaign = draftCampaign();
        campaign.setStatus(CampaignStatus.ACTIVE);
        when(campaignRepository.findByStatusInAndEndDateLessThanEqual(any(), any()))
                .thenReturn(List.of(campaign));

        ReferralLink link = new ReferralLink();
        link.setStatus(ReferralLinkStatus.ACTIVE);
        when(referralLinkRepository.findByCampaignIdAndStatusAndAssignment_Status(
                10L, ReferralLinkStatus.ACTIVE, AssignmentStatus.ACTIVE))
                .thenReturn(List.of(link));

        campaignService.expireDueCampaigns();

        assertEquals(ReferralLinkStatus.DISABLED, link.getStatus());
    }

    // --- resolveJoinLink -------------------------------------------------------------------

    @Test
    void shouldResolveJoinLinkAsOpenWhenActiveAndWithinEnrollmentWindow() {
        Campaign campaign = draftCampaign();
        campaign.setStatus(CampaignStatus.ACTIVE);
        campaign.setAmbassadorEnrollmentStart(LocalDateTime.now().minusDays(1));
        campaign.setAmbassadorEnrollmentEnd(LocalDateTime.now().plusDays(1));
        when(campaignRepository.findByCampaignCode("CODE1234AB")).thenReturn(Optional.of(campaign));

        PublicCampaignResponse response = campaignService.resolveJoinLink("CODE1234AB");

        assertTrue(response.enrollmentOpen());
        assertEquals(5L, response.companyId());
        assertEquals("Acme", response.companyName());
    }

    @Test
    void shouldAllowEnrollmentWhileScheduledIfWithinEnrollmentWindow() {
        // The whole point of a separate enrollment window: recruiting ambassadors before the
        // referral period (and therefore ACTIVE status) begins.
        Campaign campaign = draftCampaign();
        campaign.setStatus(CampaignStatus.SCHEDULED);
        campaign.setAmbassadorEnrollmentStart(LocalDateTime.now().minusDays(1));
        campaign.setAmbassadorEnrollmentEnd(LocalDateTime.now().plusDays(5));
        when(campaignRepository.findByCampaignCode("CODE1234AB")).thenReturn(Optional.of(campaign));

        PublicCampaignResponse response = campaignService.resolveJoinLink("CODE1234AB");

        assertTrue(response.enrollmentOpen());
    }

    @Test
    void shouldTreatDraftCampaignCodeAsNotFound() {
        Campaign campaign = draftCampaign();
        when(campaignRepository.findByCampaignCode("CODE1234AB")).thenReturn(Optional.of(campaign));

        assertThrows(NotFoundException.class, () -> campaignService.resolveJoinLink("CODE1234AB"));
    }

    @Test
    void shouldRejectUnknownJoinCode() {
        when(campaignRepository.findByCampaignCode("UNKNOWN")).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> campaignService.resolveJoinLink("UNKNOWN"));
    }

    @Test
    void shouldReportClosedEnrollmentForPausedCampaign() {
        Campaign campaign = draftCampaign();
        campaign.setStatus(CampaignStatus.PAUSED);
        when(campaignRepository.findByCampaignCode("CODE1234AB")).thenReturn(Optional.of(campaign));

        PublicCampaignResponse response = campaignService.resolveJoinLink("CODE1234AB");

        assertFalse(response.enrollmentOpen());
        assertEquals("Ambassador enrollment is currently paused.", response.unavailableReason());
    }

    @Test
    void shouldReportUnavailableWhenCompanyInactive() {
        company.setStatus(CompanyStatus.SUSPENDED);
        Campaign campaign = draftCampaign();
        campaign.setStatus(CampaignStatus.ACTIVE);
        campaign.setAmbassadorEnrollmentStart(LocalDateTime.now().minusDays(1));
        campaign.setAmbassadorEnrollmentEnd(LocalDateTime.now().plusDays(1));
        when(campaignRepository.findByCampaignCode("CODE1234AB")).thenReturn(Optional.of(campaign));

        PublicCampaignResponse response = campaignService.resolveJoinLink("CODE1234AB");

        assertFalse(response.enrollmentOpen());
        assertEquals("This company is not currently accepting ambassadors.", response.unavailableReason());
    }

    // --- helpers -----------------------------------------------------------------------------

    private Campaign draftCampaign() {
        Campaign campaign = new Campaign();
        campaign.setId(10L);
        campaign.setCompany(company);
        campaign.setCampaignCode("CODE1234AB");
        campaign.setName("Referral Program");
        campaign.setDescription("desc");
        campaign.setLandingPageUrl("https://acme.example.com/promo");
        campaign.setStartDate(LocalDateTime.now().minusDays(1));
        campaign.setEndDate(LocalDateTime.now().plusDays(30));
        campaign.setAmbassadorEnrollmentStart(LocalDateTime.now().minusDays(1));
        campaign.setAmbassadorEnrollmentEnd(LocalDateTime.now().plusDays(10));
        campaign.setRewardType(RewardType.DISCOUNT_AMOUNT);
        campaign.setReferrerRewardValue(BigDecimal.valueOf(20));
        campaign.setRefereeRewardValue(BigDecimal.valueOf(10));
        campaign.setConversionEventName("purchase_completed");
        campaign.setStatus(CampaignStatus.DRAFT);
        return campaign;
    }

    private CreateCampaignRequest validCreateRequest() {
        return createRequestWithDates(
                LocalDateTime.now(), LocalDateTime.now().plusDays(30),
                LocalDateTime.now(), LocalDateTime.now().plusDays(10));
    }

    private CreateCampaignRequest createRequestWithDates(
            LocalDateTime startDate, LocalDateTime endDate,
            LocalDateTime enrollmentStart, LocalDateTime enrollmentEnd) {
        return new CreateCampaignRequest(
                "Referral Program",
                "desc",
                null,
                null,
                null,
                null,
                "https://acme.example.com/promo",
                startDate,
                endDate,
                enrollmentStart,
                enrollmentEnd,
                RewardType.DISCOUNT_AMOUNT,
                BigDecimal.valueOf(20),
                BigDecimal.valueOf(10),
                "purchase_completed"
        );
    }
}
