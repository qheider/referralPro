package com.actpro.referral.revenue;

public enum AmbassadorRewardStatus {
    // Created immediately from a qualifying RevenueEvent; held here (rather than
    // auto-promoted to ELIGIBLE) when the source event's currencyMismatch flag requires
    // manual admin review - see AmbassadorReward.holdReason.
    PENDING,
    // Auto-promoted from PENDING once no hold applies; ready for admin approval.
    ELIGIBLE,
    // Admin action: approved for payout.
    APPROVED,
    // Admin action: payout recorded. Terminal - never auto-reversed even if the underlying
    // referral is later cancelled (see RevenueEventService's reversal Javadoc).
    PAID,
    // Admin action, from any pre-PAID state.
    REJECTED,
    // Automatic, driven by the matching RevenueEvent flipping to REVERSED. Only reachable from
    // PENDING/ELIGIBLE/APPROVED.
    REVERSED
}
