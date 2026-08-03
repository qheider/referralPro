package com.actpro.referral.security;

import com.actpro.referral.company.Company;
import com.actpro.referral.company.CompanyApiKeyService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ApiKeyAuthenticationFilterTest {

    @Mock
    private CompanyApiKeyService companyApiKeyService;

    private ApiKeyAuthenticationFilter filter;

    @BeforeEach
    void setUp() {
        filter = new ApiKeyAuthenticationFilter(companyApiKeyService, new ObjectMapper().registerModule(new JavaTimeModule()));
    }

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
        CompanyContext.clear();
    }

    @Test
    void shouldAuthenticateAndClearContextForValidKey() throws Exception {
        Company company = new Company();
        company.setId(5L);
        company.setName("Acme");

        when(companyApiKeyService.resolveActiveCompany("cmp_live_validsecret")).thenReturn(Optional.of(company));

        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/referrals/generate");
        request.addHeader("Authorization", "ApiKey cmp_live_validsecret");
        MockHttpServletResponse response = new MockHttpServletResponse();

        boolean[] chainInvokedWithAuthSet = new boolean[1];
        FilterChain chain = (req, res) -> {
            chainInvokedWithAuthSet[0] = SecurityContextHolder.getContext().getAuthentication() != null
                    && SecurityContextHolder.getContext().getAuthentication().isAuthenticated();
        };

        filter.doFilterInternal(request, response, chain);

        assertTrue(chainInvokedWithAuthSet[0], "authentication should be set while the chain executes");
        assertNull(SecurityContextHolder.getContext().getAuthentication(), "context must be cleared after the request");
        assertNull(CompanyContext.getCurrentCompany(), "company context must be cleared after the request");
        assertEquals(200, response.getStatus());
    }

    @Test
    void shouldReturn401ForInvalidKeyWithoutInvokingChain() throws Exception {
        when(companyApiKeyService.resolveActiveCompany("cmp_live_bogus")).thenReturn(Optional.empty());

        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/referrals/generate");
        request.addHeader("Authorization", "ApiKey cmp_live_bogus");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        filter.doFilterInternal(request, response, chain);

        assertEquals(401, response.getStatus());
        verify(chain, never()).doFilter(request, response);
    }

    @Test
    void shouldReturn401WhenAuthorizationHeaderMissing() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/referrals/generate");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        filter.doFilterInternal(request, response, chain);

        assertEquals(401, response.getStatus());
        verifyNoInteractions(companyApiKeyService);
    }

    @Test
    void shouldSkipAuthenticationForAdminEndpointsSoJwtFilterOwnsThem() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/admin/api-keys");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        filter.doFilterInternal(request, response, chain);

        verify(chain).doFilter(request, response);
        verifyNoInteractions(companyApiKeyService);
    }
}
