package com.actpro.referral.referral;

import com.actpro.referral.common.exception.NotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * Single source of truth for the URL an ambassador's {@link ReferralLink} actually resolves to,
 * and for the URL that renders its QR code - used by both {@code CampaignAssignmentService} and
 * {@code AmbassadorPortalService} so the two never drift, and reused by
 * {@code ConversionService} when it needs to reconstruct the direct-mode URL for an on-the-fly
 * {@link Referral}.
 * <p>
 * In {@link com.actpro.referral.campaign.Campaign#isDirectToLandingPageMode() direct-to-landing-page
 * mode}, the referral URL is the company's own landing page (with a {@code ?ref=} query param
 * carrying the link's {@code publicToken} for later attribution) - ReferralPro's own {@code /r/}
 * redirect is never touched. Otherwise it's today's default {@code /r/{token}} URL.
 */
@Service
@RequiredArgsConstructor
public class ReferralLinkUrlService {

    private final ReferralLinkRepository referralLinkRepository;

    @Value("${app.base-url:http://localhost:8080}")
    private String baseUrl;

    public String resolveReferralUrl(ReferralLink link) {
        if (link.getCampaign().isDirectToLandingPageMode()) {
            return appendRefParam(link.getCampaign().getLandingPageUrl(), link.getPublicToken());
        }
        return baseUrl + "/r/" + link.getPublicToken();
    }

    // Looks the link up by token and resolves its URL in one step - used by
    // ReferralRedirectController's /r/link/{token}/qrcode route so it doesn't need its own
    // repository dependency (controllers stay thin, business logic lives in services).
    public String resolveReferralUrlForToken(String publicToken) {
        ReferralLink link = referralLinkRepository.findDetailedByPublicToken(publicToken)
                .orElseThrow(() -> new NotFoundException("Referral link not found"));
        return resolveReferralUrl(link);
    }

    // Always a ReferralPro-hosted route, regardless of mode - so the frontend never needs to know
    // which mode a link is in. See ReferralRedirectController#getReferralLinkQrCode.
    public String resolveQrCodeUrl(ReferralLink link) {
        return baseUrl + "/r/link/" + link.getPublicToken() + "/qrcode";
    }

    public static String appendRefParam(String url, String refValue) {
        return url + (url.contains("?") ? "&" : "?") + "ref=" + refValue;
    }
}
