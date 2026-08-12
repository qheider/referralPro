package com.actpro.referral.conversion;

import com.actpro.referral.campaign.Campaign;
import com.actpro.referral.common.exception.BadRequestException;
import com.actpro.referral.company.Company;
import com.actpro.referral.conversion.dto.ConversionRequest;
import com.actpro.referral.referral.Referral;
import com.actpro.referral.referral.ReferralRepository;
import com.actpro.referral.reward.RewardService;
import com.actpro.referral.security.CompanyContext;
import com.actpro.referral.user.PlatformUserService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ConversionServiceTest {

    @Mock
    private ConversionRepository conversionRepository;

    @Mock
    private ReferralRepository referralRepository;

    @Mock
    private PlatformUserService platformUserService;

    @Mock
    private RewardService rewardService;

    @InjectMocks
    private ConversionService conversionService;

    private Company company;

    @BeforeEach
    void setUp() {
        company = new Company();
        company.setId(5L);
        company.setName("Acme");
        CompanyContext.setCurrentCompany(company);
    }

    @AfterEach
    void tearDown() {
        CompanyContext.clear();
    }

    @Test
    void shouldRejectConvertingAmbassadorDrivenReferral() {
        Referral referral = new Referral();
        referral.setId(9L);
        referral.setCompany(company);
        referral.setCampaign(new Campaign());
        referral.setReferralCode("LEAD1234");
        referral.setReferrerUser(null);

        when(referralRepository.findByReferralCodeAndCompanyId("LEAD1234", 5L)).thenReturn(Optional.of(referral));

        ConversionRequest request = new ConversionRequest("LEAD1234", "ext-1", "referee@example.com", "Referee", "booking_completed");

        assertThrows(BadRequestException.class, () -> conversionService.completeConversion(request));

        verifyNoInteractions(platformUserService, rewardService);
        verify(conversionRepository, never()).save(any());
    }
}
