package com.actpro.referral.integration;

import com.actpro.referral.company.Company;
import com.actpro.referral.company.CompanyIntegration;
import com.actpro.referral.company.CompanyIntegrationRepository;
import com.actpro.referral.outbox.OutboxEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ApiSubmissionServiceTest {

    @Mock
    private ApiSubmissionRepository apiSubmissionRepository;

    @Mock
    private CompanyIntegrationRepository companyIntegrationRepository;

    @InjectMocks
    private ApiSubmissionService apiSubmissionService;

    private Company company;
    private OutboxEvent event;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(apiSubmissionService, "defaultMaxRetryAttempts", 5);

        company = new Company();
        company.setId(7L);

        event = new OutboxEvent();
        event.setCompany(company);
        event.setAggregateType("REFERRAL");
        event.setAggregateId(42L);
        event.setEventType("referral.lead_registered");
    }

    @Test
    void shouldCreateNewSubmissionWithSnapshottedMaxAttempts() {
        when(apiSubmissionRepository.findByCompanyIdAndAggregateTypeAndAggregateIdAndSourceEventType(7L, "REFERRAL", 42L, "referral.lead_registered"))
                .thenReturn(Optional.empty());
        CompanyIntegration integration = new CompanyIntegration();
        integration.setMaxRetryAttempts(3);
        when(companyIntegrationRepository.findByCompanyId(7L)).thenReturn(Optional.of(integration));
        when(apiSubmissionRepository.save(any(ApiSubmission.class))).thenAnswer(inv -> inv.getArgument(0));

        ApiSubmission result = apiSubmissionService.createOrFindSubmission(event);

        ArgumentCaptor<ApiSubmission> captor = ArgumentCaptor.forClass(ApiSubmission.class);
        verify(apiSubmissionRepository).save(captor.capture());
        ApiSubmission saved = captor.getValue();
        assertEquals(ApiSubmissionStatus.PENDING, saved.getStatus());
        assertEquals(3, saved.getMaxAttempts());
        assertEquals("REFERRAL", saved.getAggregateType());
        assertEquals(42L, saved.getAggregateId());
        assertNotNull(saved.getExternalRequestId());
        assertSame(result, saved);
    }

    @Test
    void shouldFallBackToDefaultMaxAttemptsWhenNoIntegrationConfigured() {
        when(apiSubmissionRepository.findByCompanyIdAndAggregateTypeAndAggregateIdAndSourceEventType(7L, "REFERRAL", 42L, "referral.lead_registered"))
                .thenReturn(Optional.empty());
        when(companyIntegrationRepository.findByCompanyId(7L)).thenReturn(Optional.empty());
        when(apiSubmissionRepository.save(any(ApiSubmission.class))).thenAnswer(inv -> inv.getArgument(0));

        ApiSubmission result = apiSubmissionService.createOrFindSubmission(event);

        assertEquals(5, result.getMaxAttempts());
    }

    @Test
    void shouldReturnExistingSubmissionInsteadOfCreatingDuplicate() {
        ApiSubmission existing = new ApiSubmission();
        existing.setId(99L);
        when(apiSubmissionRepository.findByCompanyIdAndAggregateTypeAndAggregateIdAndSourceEventType(7L, "REFERRAL", 42L, "referral.lead_registered"))
                .thenReturn(Optional.of(existing));

        ApiSubmission result = apiSubmissionService.createOrFindSubmission(event);

        assertSame(existing, result);
        verify(apiSubmissionRepository, never()).save(any());
        verify(companyIntegrationRepository, never()).findByCompanyId(any());
    }
}
