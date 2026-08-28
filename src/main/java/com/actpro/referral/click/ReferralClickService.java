package com.actpro.referral.click;

import com.actpro.referral.common.exception.NotFoundException;
import com.actpro.referral.referral.Referral;
import com.actpro.referral.referral.ReferralLink;
import com.actpro.referral.referral.ReferralLinkRepository;
import com.actpro.referral.referral.ReferralLinkStatus;
import com.actpro.referral.referral.ReferralLinkUrlService;
import com.actpro.referral.referral.ReferralRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.HexFormat;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReferralClickService {

    private final ReferralClickRepository referralClickRepository;
    private final ReferralLinkRepository referralLinkRepository;
    private final ReferralRepository referralRepository;

    // Deliberately app.frontend-url, not app.base-url: the default destination for an
    // ambassador-link click is ReferralPro's own /refer/{token} Angular page (see recordLinkClick),
    // not a backend endpoint - same reasoning as CampaignService's joinLink.
    @Value("${app.frontend-url:http://localhost:4200}")
    private String frontendUrl;

    /**
     * Resolves a /r/{code} token against the ambassador ReferralLink model first
     * (public_token), then falls back to the legacy direct-API Referral model
     * (referral_code) so existing integrations keep working unchanged.
     */
    @Transactional
    public String resolveAndRecordClick(String code, String ipAddress, String userAgent, String refererUrl, String sessionId) {
        return referralLinkRepository.findDetailedByPublicToken(code)
                .map(link -> recordLinkClick(link, ipAddress, userAgent, refererUrl, sessionId))
                .orElseGet(() -> recordReferralClick(code, ipAddress, userAgent, refererUrl, sessionId));
    }

    private String recordLinkClick(ReferralLink link, String ipAddress, String userAgent, String refererUrl, String sessionId) {
        if (link.getStatus() != ReferralLinkStatus.ACTIVE) {
            throw new NotFoundException("Referral link not found");
        }
        if (link.getExpiresAt() != null && link.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new NotFoundException("Referral link not found");
        }

        log.info("Recording click for referral link token: {}", link.getPublicToken());

        ReferralClick click = new ReferralClick();
        click.setReferralLink(link);
        click.setCompany(link.getCompany());
        click.setCampaign(link.getCampaign());
        click.setAmbassadorUser(link.getAmbassadorUser());
        click.setSessionId(sessionId);
        click.setIpAddress(ipAddress);
        click.setIpHash(hashIp(ipAddress));
        click.setUserAgent(userAgent);
        click.setReferrerUrl(refererUrl);
        click.setClickedAt(LocalDateTime.now());
        referralClickRepository.save(click);

        referralLinkRepository.incrementClickCount(link.getId());

        // Always land on ReferralPro's own registration page first, regardless of whether the
        // link has a company-configured destinationUrl - ReferralPro needs to register the lead
        // itself (see ReferralLeadService) so click-to-registration is tracked independently of
        // the ambassador's own site. destinationUrl, when set, is only used as a post-registration
        // forward from ReferralLeadService/campaign-refer.component once the lead is captured.
        // The token is already in the path, so no ?ref= needed here. The session id lets that
        // page correlate its later lead submission (see ReferralLeadController) with this click
        // without depending on a cross-origin cookie: rp_attr_session is SameSite=Lax and set on
        // this backend's origin, so it won't be attached to the fetch/XHR POST the frontend-origin
        // page later makes back to the API.
        String destinationUrl = frontendUrl + "/refer/" + link.getPublicToken();
        return StringUtils.hasText(sessionId) ? destinationUrl + "?s=" + sessionId : destinationUrl;
    }

    private String recordReferralClick(String referralCode, String ipAddress, String userAgent, String refererUrl, String sessionId) {
        Referral referral = referralRepository.findByReferralCodeWithCampaign(referralCode)
                .orElseThrow(() -> new NotFoundException("Referral link not found"));

        log.info("Recording click for referral code: {} from IP: {}", referral.getReferralCode(), ipAddress);

        ReferralClick click = new ReferralClick();
        click.setReferral(referral);
        click.setReferralLink(referral.getReferralLinkEntity());
        click.setCompany(referral.getCompany());
        click.setCampaign(referral.getCampaign());
        click.setAmbassadorUser(referral.getAmbassadorUser());
        click.setSessionId(sessionId);
        click.setIpAddress(ipAddress);
        click.setIpHash(hashIp(ipAddress));
        click.setUserAgent(userAgent);
        click.setReferrerUrl(refererUrl);
        click.setClickedAt(LocalDateTime.now());
        referralClickRepository.save(click);

        return ReferralLinkUrlService.appendRefParam(referral.getCampaign().getLandingPageUrl(), referral.getReferralCode());
    }

    private String hashIp(String ipAddress) {
        if (!StringUtils.hasText(ipAddress)) {
            return null;
        }
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(ipAddress.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }
}
