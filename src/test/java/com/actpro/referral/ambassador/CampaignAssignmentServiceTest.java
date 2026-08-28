package com.actpro.referral.ambassador;

import com.actpro.referral.ambassador.dto.AssignAmbassadorsRequest;
import com.actpro.referral.ambassador.dto.CampaignAssignmentResponse;
import com.actpro.referral.auth.DashboardUser;
import com.actpro.referral.auth.UserRole;
import com.actpro.referral.auth.UserStatus;
import com.actpro.referral.campaign.Campaign;
import com.actpro.referral.campaign.CampaignRepository;
import com.actpro.referral.campaign.CampaignStatus;
import com.actpro.referral.common.exception.BadRequestException;
import com.actpro.referral.company.Company;
import com.actpro.referral.referral.ReferralLink;
import com.actpro.referral.referral.ReferralLinkRepository;
import com.actpro.referral.referral.ReferralLinkStatus;
import com.actpro.referral.referral.ReferralLinkUrlService;
import com.actpro.referral.referral.ReferralTokenGenerator;
import com.actpro.referral.security.CurrentUserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CampaignAssignmentServiceTest {

    @Mock
    private CampaignRepository campaignRepository;

    @Mock
    private AmbassadorProfileRepository ambassadorProfileRepository;

    @Mock
    private CampaignAmbassadorAssignmentRepository assignmentRepository;

    @Mock
    private ReferralLinkRepository referralLinkRepository;

    @Mock
    private ReferralTokenGenerator referralTokenGenerator;

    @Mock
    private CurrentUserService currentUserService;

    @Mock
    private ReferralLinkUrlService referralLinkUrlService;

    @InjectMocks
    private CampaignAssignmentService campaignAssignmentService;

    private Company company;
    private Campaign campaign;
    private DashboardUser adminUser;
    private AmbassadorProfile ambassadorProfile;

    @BeforeEach
    void setUp() {
        company = new Company();
        company.setId(5L);
        company.setName("Acme");

        campaign = new Campaign();
        campaign.setId(8L);
        campaign.setCompany(company);
        campaign.setName("Summer promo");
        campaign.setLandingPageUrl("https://campaign.example.com");
        campaign.setStatus(CampaignStatus.ACTIVE);
        campaign.setStartDate(LocalDateTime.now().minusDays(1));
        campaign.setEndDate(LocalDateTime.now().plusDays(30));

        adminUser = new DashboardUser();
        adminUser.setId(99L);
        adminUser.setCompany(company);
        adminUser.setRole(UserRole.COMPANY_ADMIN);
        adminUser.setStatus(UserStatus.ACTIVE);

        DashboardUser ambassadorUser = new DashboardUser();
        ambassadorUser.setId(21L);
        ambassadorUser.setCompany(company);
        ambassadorUser.setFirstName("Sarah");
        ambassadorUser.setLastName("Ahmed");
        ambassadorUser.setUsername("sarah@example.com");
        ambassadorUser.setRole(UserRole.AMBASSADOR);
        ambassadorUser.setStatus(UserStatus.ACTIVE);

        ambassadorProfile = new AmbassadorProfile();
        ambassadorProfile.setId(12L);
        ambassadorProfile.setCompany(company);
        ambassadorProfile.setUser(ambassadorUser);
        ambassadorProfile.setDisplayName("Sarah Travels");
        ambassadorProfile.setStatus(AmbassadorStatus.ACTIVE);

        when(currentUserService.getCurrentCompanyId()).thenReturn(5L);
        when(campaignRepository.findByIdAndCompanyId(8L, 5L)).thenReturn(Optional.of(campaign));
    }

    @Test
    void shouldCreateAssignmentAndReferralLink() {
        when(currentUserService.getCurrentUser()).thenReturn(adminUser);
        when(ambassadorProfileRepository.findDetailedByIdAndCompanyId(12L, 5L)).thenReturn(Optional.of(ambassadorProfile));
        when(assignmentRepository.findByCampaignIdAndAmbassadorUserIdAndCompanyId(8L, 21L, 5L)).thenReturn(Optional.empty());
        when(referralLinkRepository.findByCampaignIdAndAmbassadorUserIdAndCompanyId(8L, 21L, 5L)).thenReturn(Optional.empty());
        when(referralTokenGenerator.generateUniqueToken()).thenReturn("AbcDef1234567890");
        when(assignmentRepository.save(any(CampaignAmbassadorAssignment.class))).thenAnswer(invocation -> {
            CampaignAmbassadorAssignment assignment = invocation.getArgument(0);
            assignment.setId(55L);
            return assignment;
        });
        when(referralLinkRepository.save(any(ReferralLink.class))).thenAnswer(invocation -> {
            ReferralLink link = invocation.getArgument(0);
            link.setId(77L);
            return link;
        });
        when(referralLinkUrlService.resolveReferralUrl(any(ReferralLink.class)))
                .thenReturn("http://localhost:8080/r/AbcDef1234567890");
        when(referralLinkUrlService.resolveQrCodeUrl(any(ReferralLink.class)))
                .thenReturn("http://localhost:8080/r/link/AbcDef1234567890/qrcode");

        List<CampaignAssignmentResponse> response = campaignAssignmentService.assignAmbassadors(
                8L,
                new AssignAmbassadorsRequest(List.of(12L))
        );

        assertEquals(1, response.size());
        assertEquals("AbcDef1234567890", response.get(0).referralLink().publicToken());
        assertEquals("http://localhost:8080/r/AbcDef1234567890", response.get(0).referralLink().referralUrl());
        assertEquals("http://localhost:8080/r/link/AbcDef1234567890/qrcode", response.get(0).referralLink().qrCodeUrl());

        ArgumentCaptor<CampaignAmbassadorAssignment> assignmentCaptor = ArgumentCaptor.forClass(CampaignAmbassadorAssignment.class);
        verify(assignmentRepository).save(assignmentCaptor.capture());
        assertEquals(AssignmentStatus.ACTIVE, assignmentCaptor.getValue().getStatus());

        ArgumentCaptor<ReferralLink> linkCaptor = ArgumentCaptor.forClass(ReferralLink.class);
        verify(referralLinkRepository).save(linkCaptor.capture());
        assertEquals(ReferralLinkStatus.ACTIVE, linkCaptor.getValue().getStatus());
    }

    @Test
    void shouldRejectDuplicateActiveAssignment() {
        when(currentUserService.getCurrentUser()).thenReturn(adminUser);
        CampaignAmbassadorAssignment assignment = new CampaignAmbassadorAssignment();
        assignment.setId(1L);
        assignment.setStatus(AssignmentStatus.ACTIVE);

        when(ambassadorProfileRepository.findDetailedByIdAndCompanyId(12L, 5L)).thenReturn(Optional.of(ambassadorProfile));
        when(assignmentRepository.findByCampaignIdAndAmbassadorUserIdAndCompanyId(8L, 21L, 5L)).thenReturn(Optional.of(assignment));

        assertThrows(
                BadRequestException.class,
                () -> campaignAssignmentService.assignAmbassadors(8L, new AssignAmbassadorsRequest(List.of(12L)))
        );
    }

    @Test
    void shouldRemoveAssignmentAndDisableReferralLink() {
        CampaignAmbassadorAssignment assignment = new CampaignAmbassadorAssignment();
        assignment.setId(101L);
        assignment.setCompany(company);
        assignment.setCampaign(campaign);
        assignment.setAmbassadorUser(ambassadorProfile.getUser());
        assignment.setStatus(AssignmentStatus.ACTIVE);

        ReferralLink link = new ReferralLink();
        link.setId(202L);
        link.setStatus(ReferralLinkStatus.ACTIVE);

        when(ambassadorProfileRepository.findDetailedByIdAndCompanyId(12L, 5L)).thenReturn(Optional.of(ambassadorProfile));
        when(assignmentRepository.findByCampaignIdAndAmbassadorUserIdAndCompanyId(8L, 21L, 5L)).thenReturn(Optional.of(assignment));
        when(referralLinkRepository.findByCampaignIdAndAmbassadorUserIdAndCompanyId(8L, 21L, 5L)).thenReturn(Optional.of(link));

        campaignAssignmentService.removeCampaignAssignment(8L, 12L);

        assertEquals(AssignmentStatus.REMOVED, assignment.getStatus());
        assertEquals(ReferralLinkStatus.DISABLED, link.getStatus());
    }
}
