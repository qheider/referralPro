package com.actpro.referral.revenue;

import com.actpro.referral.company.Company;
import com.actpro.referral.company.CompanyIntegration;
import com.actpro.referral.company.CompanyIntegrationRepository;
import com.actpro.referral.referral.Referral;
import com.actpro.referral.referral.ReferralRepository;
import com.actpro.referral.referral.ReferralStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.EnumSet;
import java.util.Optional;
import java.util.Set;

/**
 * Core Phase 8 calculation engine, invoked by {@link RevenueEventOutboxEventHandler} - deliberately
 * has no {@code CurrentUserService} dependency, same reasoning as
 * {@code AmbassadorAdminService#autoAssignFromApplication}: this runs from the outbox dispatch
 * background thread, with no authenticated principal in context.
 * <p>
 * Idempotent per {@link Referral}: {@code uk_revenue_events_referral} means at most one
 * {@link RevenueEvent} (and therefore at most one {@link AmbassadorReward}, one-to-one via
 * {@code uk_ambassador_rewards_revenue_event}) is ever created for a given referral, regardless of
 * how many times its qualifying webhook is redelivered/reprocessed - re-invoking
 * {@link #recordQualifyingEvent} against an already-recorded referral just refreshes the recorded
 * qualifying status/occurredAt, it never creates a second reward.
 * <p>
 * Reversal ({@link #reverseIfExists}) only ever un-does a reward that hasn't been paid yet
 * (PENDING/ELIGIBLE/APPROVED -> REVERSED). A reward already PAID is left untouched - money already
 * sent isn't clawed back by this service; the underlying {@link RevenueEvent} still flips to
 * REVERSED so reporting can flag "paid then later cancelled" for manual reconciliation, but that
 * reconciliation workflow itself is out of this phase's scope (see phases_tracker.txt's Payout
 * tracking note).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RevenueEventService {

    private static final Set<AmbassadorRewardStatus> REVERSIBLE_REWARD_STATUSES =
            EnumSet.of(AmbassadorRewardStatus.PENDING, AmbassadorRewardStatus.ELIGIBLE, AmbassadorRewardStatus.APPROVED);

    private final ReferralRepository referralRepository;
    private final CompanyIntegrationRepository companyIntegrationRepository;
    private final RevenueEventRepository revenueEventRepository;
    private final AmbassadorRewardRepository ambassadorRewardRepository;
    private final RewardStatusMappingService rewardStatusMappingService;

    @Transactional
    public void applyReferralStatusChange(Company company, Long referralId, BigDecimal revenueAmount, String currency, LocalDateTime occurredAt) {
        Referral referral = referralRepository.findById(referralId).orElse(null);
        if (referral == null) {
            log.warn("referral.status_changed event for referral {} skipped: referral no longer exists", referralId);
            return;
        }

        // Ambassador-attribution only, same scope boundary ConversionService already draws for
        // the legacy flow the other direction ("Ambassador-driven referrals cannot be converted
        // yet"). A legacy direct-API referral never reaches here in practice (it has no
        // ApiSubmission/webhook plumbing to begin with - see WebhookProcessingService's reference
        // matching), this is a defensive no-op, not a normal path.
        if (referral.getAmbassadorUser() == null) {
            return;
        }

        ReferralStatus currentStatus = referral.getStatus();
        String rewardMappingJson = companyIntegrationRepository.findByCompanyId(company.getId())
                .map(CompanyIntegration::getRewardMappingJson)
                .orElse(null);
        RewardMappingClassification classification = rewardStatusMappingService.classify(rewardMappingJson, currentStatus);

        switch (classification) {
            case QUALIFYING -> recordQualifyingEvent(referral, currentStatus, revenueAmount, currency, occurredAt);
            case REVERSING -> reverseIfExists(referral);
            case IGNORE -> { /* no revenue/reward consequence for this status */ }
        }
    }

    /**
     * Ambassador-side counterpart to {@code ConversionService.completeConversion} for a company's
     * direct API report of a registration/conversion (as opposed to a webhook-reported status
     * change - see {@link #applyReferralStatusChange}) - lets a direct-to-landing-page ambassador
     * referral (no {@code ApiSubmission}/webhook plumbing, since ReferralPro never captured the
     * lead itself) still produce an {@link AmbassadorReward} through the same idempotent machinery.
     * {@code revenueAmount}/{@code currency} are always null here: {@code ConversionRequest} carries
     * neither, unlike the webhook payload.
     */
    @Transactional
    public AmbassadorReward recordConversionQualifyingEvent(Referral referral, LocalDateTime occurredAt) {
        // Defensive - every caller (ConversionService) only reaches this on the ambassador-driven
        // branch (referrerUser == null), where ambassadorUser is always set, but this guards
        // against a future caller passing a legacy referral without it.
        if (referral.getAmbassadorUser() == null) {
            throw new IllegalStateException("Cannot record a conversion qualifying event for a referral with no ambassadorUser: " + referral.getId());
        }
        return recordQualifyingEvent(referral, referral.getStatus(), null, null, occurredAt);
    }

    private AmbassadorReward recordQualifyingEvent(Referral referral, ReferralStatus currentStatus, BigDecimal revenueAmount, String currency, LocalDateTime occurredAt) {
        Optional<RevenueEvent> existing = revenueEventRepository.findByReferralId(referral.getId());
        if (existing.isPresent()) {
            RevenueEvent revenueEvent = existing.get();
            if (revenueEvent.getStatus() == RevenueEventStatus.RECORDED) {
                // Referral progressed further along the qualifying set (e.g. COMPLETED -> CONVERTED) -
                // refresh which status/when, but never touch the already-created reward: the reward
                // rule was snapshotted once, at first qualification, and stays fixed.
                revenueEvent.setQualifyingStatus(currentStatus.name());
                revenueEvent.setOccurredAt(occurredAt != null ? occurredAt : LocalDateTime.now());
                revenueEventRepository.save(revenueEvent);
            } else {
                log.info("Referral {} reported a qualifying status again after its RevenueEvent was already REVERSED - ignoring", referral.getId());
            }
            return ambassadorRewardRepository.findByRevenueEventId(revenueEvent.getId()).orElse(null);
        }

        Company company = referral.getCompany();
        String companyCurrency = company.getPreferredCurrency();
        boolean currencyMismatch = currency != null && companyCurrency != null && !currency.equalsIgnoreCase(companyCurrency);

        RevenueEvent revenueEvent = new RevenueEvent();
        revenueEvent.setCompany(company);
        revenueEvent.setCampaign(referral.getCampaign());
        revenueEvent.setReferral(referral);
        revenueEvent.setAmbassadorUser(referral.getAmbassadorUser());
        revenueEvent.setQualifyingStatus(currentStatus.name());
        revenueEvent.setOccurredAt(occurredAt != null ? occurredAt : LocalDateTime.now());
        revenueEvent.setRevenueAmount(revenueAmount);
        revenueEvent.setCurrency(currency);
        revenueEvent.setCurrencyMismatch(currencyMismatch);
        revenueEvent.setStatus(RevenueEventStatus.RECORDED);
        revenueEvent = revenueEventRepository.save(revenueEvent);

        AmbassadorReward reward = new AmbassadorReward();
        reward.setCompany(company);
        reward.setCampaign(referral.getCampaign());
        reward.setReferral(referral);
        reward.setRevenueEvent(revenueEvent);
        reward.setAmbassadorUser(referral.getAmbassadorUser());
        reward.setRewardType(referral.getCampaign().getRewardType());
        reward.setRewardValue(referral.getCampaign().getReferrerRewardValue());
        reward.setCurrency(companyCurrency);
        if (currencyMismatch) {
            reward.setStatus(AmbassadorRewardStatus.PENDING);
            reward.setHoldReason("CURRENCY_MISMATCH");
        } else {
            reward.setStatus(AmbassadorRewardStatus.ELIGIBLE);
        }
        reward = ambassadorRewardRepository.save(reward);

        log.info("Recorded RevenueEvent {} and AmbassadorReward for referral {} (ambassador {}, campaign {})",
                revenueEvent.getId(), referral.getId(), referral.getAmbassadorUser().getId(), referral.getCampaign().getId());
        return reward;
    }

    private void reverseIfExists(Referral referral) {
        Optional<RevenueEvent> existing = revenueEventRepository.findByReferralId(referral.getId());
        if (existing.isEmpty()) {
            return;
        }
        RevenueEvent revenueEvent = existing.get();
        if (revenueEvent.getStatus() == RevenueEventStatus.REVERSED) {
            return;
        }
        revenueEvent.setStatus(RevenueEventStatus.REVERSED);
        revenueEvent.setReversedAt(LocalDateTime.now());
        revenueEventRepository.save(revenueEvent);

        ambassadorRewardRepository.findByRevenueEventId(revenueEvent.getId()).ifPresent(reward -> {
            if (REVERSIBLE_REWARD_STATUSES.contains(reward.getStatus())) {
                reward.setStatus(AmbassadorRewardStatus.REVERSED);
                reward.setReversedAt(LocalDateTime.now());
                ambassadorRewardRepository.save(reward);
            } else {
                log.info("RevenueEvent {} reversed but its AmbassadorReward {} is already {} - left untouched",
                        revenueEvent.getId(), reward.getId(), reward.getStatus());
            }
        });
    }
}
