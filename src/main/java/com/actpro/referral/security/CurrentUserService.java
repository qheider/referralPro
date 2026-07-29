package com.actpro.referral.security;

import com.actpro.referral.ambassador.AmbassadorProfile;
import com.actpro.referral.ambassador.AmbassadorProfileRepository;
import com.actpro.referral.auth.DashboardUser;
import com.actpro.referral.auth.DashboardUserRepository;
import com.actpro.referral.auth.UserRole;
import com.actpro.referral.common.exception.UnauthorizedException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CurrentUserService {

    private final DashboardUserRepository dashboardUserRepository;
    private final AmbassadorProfileRepository ambassadorProfileRepository;

    @Transactional(readOnly = true)
    public DashboardUser getCurrentUser() {
        AuthenticatedUser authenticatedUser = requireAuthenticatedUser();
        return dashboardUserRepository.findByIdWithCompany(authenticatedUser.userId())
                .orElseThrow(() -> new UnauthorizedException("Authenticated user not found"));
    }

    public Long getCurrentUserId() {
        return requireAuthenticatedUser().userId();
    }

    public Long getCurrentCompanyId() {
        return requireAuthenticatedUser().companyId();
    }

    public UserRole getCurrentUserRole() {
        return requireAuthenticatedUser().role();
    }

    @Transactional(readOnly = true)
    public Long getCurrentAmbassadorId() {
        if (getCurrentUserRole() != UserRole.AMBASSADOR) {
            throw new AccessDeniedException("Current user is not an ambassador");
        }

        return ambassadorProfileRepository.findByUserId(getCurrentUserId())
                .map(AmbassadorProfile::getId)
                .orElseThrow(() -> new UnauthorizedException("Ambassador profile not found"));
    }

    @Transactional(readOnly = true)
    public AmbassadorProfile getCurrentAmbassadorProfile() {
        if (getCurrentUserRole() != UserRole.AMBASSADOR) {
            throw new AccessDeniedException("Current user is not an ambassador");
        }

        return ambassadorProfileRepository
                .findDetailedByCompanyIdAndUserId(getCurrentCompanyId(), getCurrentUserId())
                .orElseThrow(() -> new UnauthorizedException("Ambassador profile not found"));
    }

    public void assertCurrentCompanyAccess(Long requestedCompanyId) {
        if (!getCurrentCompanyId().equals(requestedCompanyId)) {
            throw new AccessDeniedException("Cannot access another company's data");
        }
    }

    private AuthenticatedUser requireAuthenticatedUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new UnauthorizedException("Authentication is required");
        }

        Object principal = authentication.getPrincipal();
        if (principal instanceof AuthenticatedUser authenticatedUser) {
            return authenticatedUser;
        }

        throw new UnauthorizedException("Unsupported authentication principal");
    }
}
