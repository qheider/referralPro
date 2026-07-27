package com.actpro.referral.security;

import com.actpro.referral.ambassador.AmbassadorProfile;
import com.actpro.referral.ambassador.AmbassadorProfileRepository;
import com.actpro.referral.auth.DashboardUserRepository;
import com.actpro.referral.auth.UserRole;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CurrentUserServiceTest {

    @Mock
    private DashboardUserRepository dashboardUserRepository;

    @Mock
    private AmbassadorProfileRepository ambassadorProfileRepository;

    @InjectMocks
    private CurrentUserService currentUserService;

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void shouldReturnAuthenticatedCompanyAndUserDetails() {
        setAuthenticatedUser(UserRole.COMPANY_ADMIN);

        assertEquals(7L, currentUserService.getCurrentUserId());
        assertEquals(11L, currentUserService.getCurrentCompanyId());
        assertEquals(UserRole.COMPANY_ADMIN, currentUserService.getCurrentUserRole());
    }

    @Test
    void shouldResolveCurrentAmbassadorProfileId() {
        setAuthenticatedUser(UserRole.AMBASSADOR);

        AmbassadorProfile profile = new AmbassadorProfile();
        profile.setId(23L);
        when(ambassadorProfileRepository.findByUserId(7L)).thenReturn(Optional.of(profile));

        assertEquals(23L, currentUserService.getCurrentAmbassadorId());
    }

    @Test
    void shouldRejectAmbassadorResolutionForNonAmbassadorUsers() {
        setAuthenticatedUser(UserRole.COMPANY_ADMIN);

        assertThrows(AccessDeniedException.class, () -> currentUserService.getCurrentAmbassadorId());
    }

    @Test
    void shouldRejectCrossCompanyAccess() {
        setAuthenticatedUser(UserRole.COMPANY_ADMIN);

        assertThrows(AccessDeniedException.class, () -> currentUserService.assertCurrentCompanyAccess(12L));
    }

    private void setAuthenticatedUser(UserRole role) {
        AuthenticatedUser principal = new AuthenticatedUser(7L, "user@example.com", 11L, role);
        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(principal, null, List.of());
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }
}
