package com.actpro.referral.referral;

import com.actpro.referral.click.ReferralClickService;
import com.actpro.referral.common.exception.NotFoundException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.servlet.view.RedirectView;

@Tag(name = "Referral Redirect", description = "Public referral link redirect")
@Controller
@RequiredArgsConstructor
public class ReferralRedirectController {

    private final ReferralClickService referralClickService;
    private final ReferralRepository referralRepository;

    @Operation(
            summary = "Referral redirect",
            description = "Public endpoint to track clicks and redirect to campaign landing page"
    )
    @GetMapping("/r/{referralCode}")
    public RedirectView handleReferralRedirect(
            @PathVariable String referralCode,
            HttpServletRequest request) {

        // Find referral with campaign eagerly loaded
        Referral referral = referralRepository.findByReferralCodeWithCampaign(referralCode)
                .orElseThrow(() -> new NotFoundException("Referral code not found"));

        // Get IP and user agent
        String ipAddress = getClientIp(request);
        String userAgent = request.getHeader("User-Agent");

        // Record click
        referralClickService.recordClick(referral, ipAddress, userAgent);

        // Redirect to landing page with referral code
        String landingPageUrl = referral.getCampaign().getLandingPageUrl();
        String redirectUrl = landingPageUrl + (landingPageUrl.contains("?") ? "&" : "?") + "ref=" + referralCode;

        return new RedirectView(redirectUrl);
    }

    private String getClientIp(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
