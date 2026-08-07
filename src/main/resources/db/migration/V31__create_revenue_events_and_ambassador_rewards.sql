-- Phase 8 (revenue, rewards, and dashboards): the ambassador-attribution counterpart to the
-- legacy reward.Reward/Conversion pipeline. That pipeline is keyed off PlatformUser (referrer)
-- and a synchronous ConversionController call; ambassador-driven referrals have no PlatformUser
-- referrer (see referrals.referrer_user_id's nullability, phase 4/5) and their qualifying signal
-- is now the Phase 7 webhook-driven Referral.status transition instead of a direct API call.
--
-- revenue_events is the durable, idempotent record of "this referral reached a qualifying
-- status" - one row per referral (uk_revenue_events_referral), created/reversed by
-- revenue.RevenueEventService reacting to the referral.status_changed outbox event
-- WebhookProcessingService now publishes. ambassador_rewards is the payable reward calculated
-- from it, with campaign reward-rule values snapshotted at creation time so a later campaign
-- edit never rewrites a historical reward (same "snapshot, don't recompute" principle CampaignService
-- already applies via its DRAFT-only reward-field lock).
CREATE TABLE revenue_events (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    company_id BIGINT NOT NULL,
    campaign_id BIGINT NOT NULL,
    referral_id BIGINT NOT NULL,
    ambassador_user_id BIGINT NOT NULL,
    qualifying_status VARCHAR(30) NOT NULL,
    occurred_at TIMESTAMP NOT NULL,
    -- Parsed from the webhook's revenueAmount/currency fields (stored verbatim in raw_payload
    -- since Phase 7 - see webhook_events comment). Informational only: the ambassador_rewards
    -- payout amount always comes from the campaign's snapshotted reward rule below, never from
    -- this figure, so a currency mismatch here can never corrupt payout math.
    revenue_amount DECIMAL(12, 2) NULL,
    currency VARCHAR(10) NULL,
    -- Set when currency is present but doesn't match companies.preferred_currency - the "explicit
    -- validation policy" for currency mismatch: never silently blend mismatched-currency amounts
    -- into a summed total (RevenueAdminService reporting excludes flagged rows from currency
    -- totals) and hold the resulting reward in PENDING for manual admin review (see
    -- ambassador_rewards.hold_reason) rather than silently auto-approving it.
    currency_mismatch BOOLEAN NOT NULL DEFAULT FALSE,
    status VARCHAR(20) NOT NULL DEFAULT 'RECORDED',
    reversed_at TIMESTAMP NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_revenue_events_company FOREIGN KEY (company_id) REFERENCES companies(id) ON DELETE CASCADE,
    CONSTRAINT fk_revenue_events_campaign FOREIGN KEY (campaign_id) REFERENCES campaigns(id) ON DELETE CASCADE,
    -- RESTRICT, not CASCADE (unlike the FKs above): this is a financial/audit record, not a
    -- derived one. Nothing in the app hard-deletes a Referral today, but if that ever changes,
    -- a row here must block it rather than silently vanish a reward that may already be PAID -
    -- see the status column comment on ambassador_rewards below.
    CONSTRAINT fk_revenue_events_referral FOREIGN KEY (referral_id) REFERENCES referrals(id) ON DELETE RESTRICT,
    CONSTRAINT fk_revenue_events_ambassador FOREIGN KEY (ambassador_user_id) REFERENCES dashboard_users(id) ON DELETE CASCADE,
    -- The idempotency key: RevenueEventService.recordQualifyingEvent never creates a second row
    -- for a referral that already has one, regardless of how many times the qualifying webhook
    -- event is redelivered/reprocessed.
    CONSTRAINT uk_revenue_events_referral UNIQUE (referral_id),
    INDEX idx_revenue_events_company_id (company_id),
    INDEX idx_revenue_events_campaign_id (campaign_id),
    INDEX idx_revenue_events_ambassador (ambassador_user_id, company_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE ambassador_rewards (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    company_id BIGINT NOT NULL,
    campaign_id BIGINT NOT NULL,
    referral_id BIGINT NOT NULL,
    revenue_event_id BIGINT NOT NULL,
    ambassador_user_id BIGINT NOT NULL,
    reward_type VARCHAR(30) NOT NULL,
    reward_value DECIMAL(10, 2) NOT NULL,
    currency VARCHAR(10) NULL,
    -- PENDING -> ELIGIBLE happens automatically unless currency_mismatch held it back;
    -- ELIGIBLE -> APPROVED -> PAID are admin actions (RevenueAdminController); REJECTED is an
    -- admin action from any pre-PAID state; REVERSED is automatic, driven by the matching
    -- revenue_events row flipping to REVERSED (a later CANCELLED/REJECTED webhook). A reward
    -- already PAID is never auto-reversed - money already sent isn't clawed back by this table;
    -- see revenue.RevenueEventService's Javadoc for the reconciliation note.
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    hold_reason VARCHAR(100) NULL,
    approved_at TIMESTAMP NULL,
    paid_at TIMESTAMP NULL,
    rejected_at TIMESTAMP NULL,
    reversed_at TIMESTAMP NULL,
    rejection_reason VARCHAR(500) NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_ambassador_rewards_company FOREIGN KEY (company_id) REFERENCES companies(id) ON DELETE CASCADE,
    CONSTRAINT fk_ambassador_rewards_campaign FOREIGN KEY (campaign_id) REFERENCES campaigns(id) ON DELETE CASCADE,
    -- RESTRICT, same reasoning as fk_revenue_events_referral above - and RESTRICT on the
    -- revenue_event_id link too, so a RevenueEvent behind a PAID reward can't be cascade-deleted
    -- out from under it either.
    CONSTRAINT fk_ambassador_rewards_referral FOREIGN KEY (referral_id) REFERENCES referrals(id) ON DELETE RESTRICT,
    CONSTRAINT fk_ambassador_rewards_revenue_event FOREIGN KEY (revenue_event_id) REFERENCES revenue_events(id) ON DELETE RESTRICT,
    CONSTRAINT fk_ambassador_rewards_ambassador FOREIGN KEY (ambassador_user_id) REFERENCES dashboard_users(id) ON DELETE CASCADE,
    CONSTRAINT uk_ambassador_rewards_revenue_event UNIQUE (revenue_event_id),
    INDEX idx_ambassador_rewards_company_id (company_id),
    INDEX idx_ambassador_rewards_campaign_id (campaign_id),
    INDEX idx_ambassador_rewards_ambassador (ambassador_user_id, company_id),
    INDEX idx_ambassador_rewards_status (status),
    -- Backs AmbassadorRewardRepository's company+status and company+campaign(+status) admin
    -- listing queries - same rationale as V24's idx_ambassador_applications_company_status.
    INDEX idx_ambassador_rewards_company_status (company_id, status),
    INDEX idx_ambassador_rewards_company_campaign (company_id, campaign_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
