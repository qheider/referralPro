package com.actpro.referral.referral;

import com.actpro.referral.click.ReferralClick;
import com.actpro.referral.click.ReferralClickRepository;
import com.actpro.referral.common.exception.BadRequestException;
import com.actpro.referral.common.exception.NotFoundException;
import com.actpro.referral.company.Company;
import com.actpro.referral.outbox.OutboxEventPublisher;
import com.actpro.referral.referral.dto.SubmitReferralLeadRequest;
import com.actpro.referral.referral.dto.SubmitReferralLeadResponse;
import com.actpro.referral.user.PlatformUser;
import com.actpro.referral.user.PlatformUserService;
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
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;

/**
 * Public, unauthenticated lead capture for the ambassador-link flow: turns a click on
 * {@code GET /r/{token}} into a {@link Referral} once the visitor identifies themselves.
 * Deliberately scoped to the ReferralLink/publicToken model only - a legacy direct-API referral
 * (created by {@link ReferralService#generateReferral}) already exists the moment the company
 * calls that endpoint, so "submit a lead against it" doesn't apply there.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ReferralLeadService {

    private static final Set<ReferralStatus> TERMINAL_STATUSES =
            Set.of(ReferralStatus.REJECTED, ReferralStatus.EXPIRED, ReferralStatus.CANCELLED);

    private final ReferralLinkRepository referralLinkRepository;
    private final ReferralRepository referralRepository;
    private final ReferralClickRepository referralClickRepository;
    private final PlatformUserService platformUserService;
    private final ReferralCodeGenerator referralCodeGenerator;
    private final OutboxEventPublisher outboxEventPublisher;

    @Value("${app.base-url}")
    private String baseUrl;

    @Transactional
    public SubmitReferralLeadResponse submitLead(String token, String sessionId, SubmitReferralLeadRequest request) {
        ReferralLink link = referralLinkRepository.findDetailedByPublicToken(token)
                .orElseThrow(() -> new NotFoundException("Referral link not found"));

        if (link.getStatus() != ReferralLinkStatus.ACTIVE) {
            throw new NotFoundException("Referral link not found");
        }
        if (link.getExpiresAt() != null && link.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new NotFoundException("Referral link not found");
        }

        if (sessionId != null) {
            Optional<Referral> existing = referralRepository.findByReferralLinkEntityIdAndAttributionSessionId(link.getId(), sessionId);
            if (existing.isPresent()) {
                log.info("Returning existing referral for session {} on link {}", sessionId, link.getPublicToken());
                return toResponse(existing.get(), link);
            }
        }

        String normalizedEmail = request.email().trim().toLowerCase(Locale.ROOT);

        boolean duplicate = referralRepository.existsByReferralLinkEntityIdAndCustomerUserEmailAndStatusNotIn(
                link.getId(), normalizedEmail, TERMINAL_STATUSES);
        if (duplicate) {
            throw new BadRequestException("A submission for this email is already in progress for this referral link");
        }

        Company company = link.getCompany();
        PlatformUser customer = platformUserService.findOrCreate(
                company,
                syntheticExternalUserId(normalizedEmail),
                normalizedEmail,
                request.name().trim()
        );

        String code = referralCodeGenerator.generateUniqueCode();

        Referral referral = new Referral();
        referral.setCompany(company);
        referral.setCampaign(link.getCampaign());
        referral.setAmbassadorUser(link.getAmbassadorUser());
        referral.setReferralLinkEntity(link);
        referral.setCustomerUser(customer);
        referral.setReferralCode(code);
        referral.setReferralLink(baseUrl + "/r/" + link.getPublicToken());
        referral.setStatus(ReferralStatus.REGISTERED);
        referral.setRegisteredAt(LocalDateTime.now());
        referral.setAttributionSessionId(sessionId);
        referral = referralRepository.save(referral);

        if (sessionId != null) {
            List<ReferralClick> clicks = referralClickRepository.findByReferralLinkIdAndSessionIdAndReferralIsNull(link.getId(), sessionId);
            for (ReferralClick click : clicks) {
                click.setReferral(referral);
            }
        }

        log.info("Registered lead referral {} for link {}", code, link.getPublicToken());
        publishEvent(referral);

        return toResponse(referral, link);
    }

    private void publishEvent(Referral referral) {
        ReferralLeadEventPayload payload = new ReferralLeadEventPayload(
                referral.getId(),
                referral.getReferralCode(),
                referral.getCompany().getId(),
                referral.getCampaign().getId(),
                referral.getAmbassadorUser() != null ? referral.getAmbassadorUser().getId() : null,
                referral.getCustomerUser().getEmail(),
                referral.getStatus(),
                LocalDateTime.now()
        );
        outboxEventPublisher.publish(referral.getCompany(), "REFERRAL", referral.getId(), "referral.lead_registered", payload);
    }

    private SubmitReferralLeadResponse toResponse(Referral referral, ReferralLink link) {
        String redirectUrl = StringUtils.hasText(link.getDestinationUrl())
                ? appendRefParam(link.getDestinationUrl(), referral.getReferralCode())
                : null;
        return new SubmitReferralLeadResponse(referral.getReferralCode(), referral.getStatus(), referral.getRegisteredAt(), redirectUrl);
    }

    private String appendRefParam(String url, String refCode) {
        return url + (url.contains("?") ? "&" : "?") + "ref=" + refCode;
    }

    private String syntheticExternalUserId(String normalizedEmail) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(normalizedEmail.getBytes(StandardCharsets.UTF_8));
            return "lead:" + HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }

    private record ReferralLeadEventPayload(
            Long referralId,
            String referralCode,
            Long companyId,
            Long campaignId,
            Long ambassadorUserId,
            String email,
            ReferralStatus status,
            LocalDateTime occurredAt
    ) {
    }
}
