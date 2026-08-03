package com.actpro.referral.referral;

import com.actpro.referral.ambassador.AssignmentStatus;
import com.actpro.referral.ambassador.CampaignAmbassadorAssignment;
import com.actpro.referral.ambassador.CampaignAmbassadorAssignmentRepository;
import com.actpro.referral.auth.DashboardUser;
import com.actpro.referral.auth.DashboardUserRepository;
import com.actpro.referral.auth.UserRole;
import com.actpro.referral.auth.UserStatus;
import com.actpro.referral.campaign.Campaign;
import com.actpro.referral.campaign.CampaignRepository;
import com.actpro.referral.campaign.CampaignStatus;
import com.actpro.referral.campaign.RewardType;
import com.actpro.referral.company.Company;
import com.actpro.referral.company.CompanyRepository;
import com.actpro.referral.company.CompanyStatus;
import com.actpro.referral.config.JpaConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test")
@Import(JpaConfig.class)
class ReferralLinkRepositoryTest {

    @Autowired
    private ReferralLinkRepository referralLinkRepository;

    @Autowired
    private CompanyRepository companyRepository;

    @Autowired
    private CampaignRepository campaignRepository;

    @Autowired
    private DashboardUserRepository dashboardUserRepository;

    @Autowired
    private CampaignAmbassadorAssignmentRepository assignmentRepository;

    private Campaign campaign;
    private DashboardUser ambassadorUser;

    @BeforeEach
    void setUp() {
        Company company = new Company();
        company.setName("Acme");
        company.setEmail("acme@example.com");
        company.setStatus(CompanyStatus.ACTIVE);
        company = companyRepository.save(company);

        campaign = new Campaign();
        campaign.setCompany(company);
        campaign.setName("Summer promo");
        campaign.setLandingPageUrl("https://campaign.example.com");
        campaign.setStartDate(LocalDateTime.now().minusDays(1));
        campaign.setEndDate(LocalDateTime.now().plusDays(30));
        campaign.setRewardType(RewardType.DISCOUNT_PERCENTAGE);
        campaign.setReferrerRewardValue(BigDecimal.TEN);
        campaign.setRefereeRewardValue(BigDecimal.TEN);
        campaign.setConversionEventName("FIRST_RIDE_COMPLETED");
        campaign.setStatus(CampaignStatus.ACTIVE);
        campaign = campaignRepository.save(campaign);

        ambassadorUser = new DashboardUser();
        ambassadorUser.setCompany(company);
        ambassadorUser.setUsername("sarah@example.com");
        ambassadorUser.setPassword("encoded");
        ambassadorUser.setRole(UserRole.AMBASSADOR);
        ambassadorUser.setStatus(UserStatus.ACTIVE);
        ambassadorUser = dashboardUserRepository.save(ambassadorUser);

        CampaignAmbassadorAssignment assignment = new CampaignAmbassadorAssignment();
        assignment.setCompany(company);
        assignment.setCampaign(campaign);
        assignment.setAmbassadorUser(ambassadorUser);
        assignment.setStatus(AssignmentStatus.ACTIVE);
        assignmentRepository.save(assignment);

        ReferralLink link = new ReferralLink();
        link.setCompany(company);
        link.setCampaign(campaign);
        link.setAmbassadorUser(ambassadorUser);
        link.setAssignment(assignment);
        link.setPublicToken("AbcDef1234567890");
        link.setStatus(ReferralLinkStatus.ACTIVE);
        link.setClickCount(0L);
        referralLinkRepository.save(link);
    }

    @Test
    void shouldFindDetailedByPublicTokenWithCampaignLoaded() {
        Optional<ReferralLink> result = referralLinkRepository.findDetailedByPublicToken("AbcDef1234567890");

        assertTrue(result.isPresent());
        assertEquals("https://campaign.example.com", result.get().getCampaign().getLandingPageUrl());
    }

    @Test
    void shouldReturnEmptyForUnknownPublicToken() {
        assertTrue(referralLinkRepository.findDetailedByPublicToken("does-not-exist").isEmpty());
    }

    @Test
    void shouldIncrementClickCountAtomically() {
        ReferralLink link = referralLinkRepository.findByPublicToken("AbcDef1234567890").orElseThrow();

        referralLinkRepository.incrementClickCount(link.getId());
        referralLinkRepository.incrementClickCount(link.getId());

        ReferralLink reloaded = referralLinkRepository.findById(link.getId()).orElseThrow();
        assertEquals(2L, reloaded.getClickCount());
    }
}
