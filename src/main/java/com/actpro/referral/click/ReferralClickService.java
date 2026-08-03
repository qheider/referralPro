package com.actpro.referral.click;

import com.actpro.referral.common.exception.NotFoundException;
import com.actpro.referral.referral.Referral;
import com.actpro.referral.referral.ReferralLink;
import com.actpro.referral.referral.ReferralLinkRepository;
import com.actpro.referral.referral.ReferralLinkStatus;
import com.actpro.referral.referral.ReferralRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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

        String destinationUrl = StringUtils.hasText(link.getDestinationUrl())
                ? link.getDestinationUrl()
                : link.getCampaign().getLandingPageUrl();
        return appendRefParam(destinationUrl, link.getPublicToken());
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

        return appendRefParam(referral.getCampaign().getLandingPageUrl(), referral.getReferralCode());
    }

    private String appendRefParam(String url, String refCode) {
        return url + (url.contains("?") ? "&" : "?") + "ref=" + refCode;
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
