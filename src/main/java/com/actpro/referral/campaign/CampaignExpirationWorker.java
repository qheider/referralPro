package com.actpro.referral.campaign;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Applies the two automatic campaign lifecycle transitions - SCHEDULED -> ACTIVE and
 * ACTIVE/PAUSED -> EXPIRED - on a poll interval. Manual transitions (publish/pause/resume/close/
 * archive) go through CampaignController instead. Same disable-in-tests shape as
 * outbox/OutboxDispatcher.
 */
@Component
@RequiredArgsConstructor
@Slf4j
@ConditionalOnProperty(name = "app.campaign.expiration-enabled", havingValue = "true", matchIfMissing = true)
public class CampaignExpirationWorker {

    private final CampaignService campaignService;

    @Scheduled(fixedDelayString = "${app.campaign.expiration-interval-ms:60000}")
    public void applyDueTransitions() {
        int activated = campaignService.activateScheduledCampaigns();
        if (activated > 0) {
            log.info("Activated {} scheduled campaign(s)", activated);
        }

        int expired = campaignService.expireDueCampaigns();
        if (expired > 0) {
            log.info("Expired {} campaign(s) past their referral end date", expired);
        }
    }
}
