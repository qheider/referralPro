package com.actpro.referral.outbox;

public enum OutboxEventStatus {
    PENDING,
    PROCESSING,
    PUBLISHED,
    DEAD_LETTER
}
