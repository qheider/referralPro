package com.actpro.referral.revenue;

import com.actpro.referral.referral.ReferralStatus;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.EnumMap;
import java.util.Map;
import java.util.Optional;

/**
 * Parses {@code CompanyIntegration.rewardMappingJson} (mapped-ReferralStatus-name ->
 * {@link RewardMappingClassification} name) - the field Phase 6 seeded and syntax-validated but
 * left "opaque... unused until Phase 8". Lets a company override which of its already-mapped
 * {@code ReferralStatusMappingService} outcomes actually trigger revenue/reward creation
 * (QUALIFYING), reversal (REVERSING), or neither (IGNORE) - e.g. a company that wants to pay out
 * as soon as a booking is confirmed rather than waiting for completion can map
 * {@code BOOKING_CONFIRMED: "QUALIFYING"}.
 * <p>
 * Unset/empty json, or a mapped status absent from it, falls back to
 * {@link #DEFAULT_CLASSIFICATION}: COMPLETED/CONVERTED are QUALIFYING, CANCELLED/REJECTED are
 * REVERSING, everything else is IGNORE - matching {@code AmbassadorPortalService}'s existing
 * {@code COMPLETED_RENTAL_STATUSES} definition of "done".
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class RewardStatusMappingService {

    private static final Map<ReferralStatus, RewardMappingClassification> DEFAULT_CLASSIFICATION = new EnumMap<>(ReferralStatus.class);

    static {
        DEFAULT_CLASSIFICATION.put(ReferralStatus.COMPLETED, RewardMappingClassification.QUALIFYING);
        DEFAULT_CLASSIFICATION.put(ReferralStatus.CONVERTED, RewardMappingClassification.QUALIFYING);
        DEFAULT_CLASSIFICATION.put(ReferralStatus.CANCELLED, RewardMappingClassification.REVERSING);
        DEFAULT_CLASSIFICATION.put(ReferralStatus.REJECTED, RewardMappingClassification.REVERSING);
    }

    private final ObjectMapper objectMapper;

    /** Validates every key/value in a candidate rewardMappingJson. Returns the first invalid entry found. */
    public Optional<String> findFirstInvalidMappingEntry(String rewardMappingJson) {
        Map<String, String> mapping = parse(rewardMappingJson);
        for (Map.Entry<String, String> entry : mapping.entrySet()) {
            if (!isValidStatus(entry.getKey())) {
                return Optional.of(entry.getKey());
            }
            if (!isValidClassification(entry.getValue())) {
                return Optional.of(entry.getValue());
            }
        }
        return Optional.empty();
    }

    public RewardMappingClassification classify(String rewardMappingJson, ReferralStatus status) {
        Map<String, String> mapping = parse(rewardMappingJson);
        String override = mapping.get(status.name());
        if (override != null && isValidClassification(override)) {
            return RewardMappingClassification.valueOf(override);
        }
        return DEFAULT_CLASSIFICATION.getOrDefault(status, RewardMappingClassification.IGNORE);
    }

    private boolean isValidStatus(String value) {
        try {
            ReferralStatus.valueOf(value);
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    private boolean isValidClassification(String value) {
        try {
            RewardMappingClassification.valueOf(value);
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    private Map<String, String> parse(String rewardMappingJson) {
        if (rewardMappingJson == null || rewardMappingJson.isBlank()) {
            return Map.of();
        }
        try {
            return objectMapper.readValue(rewardMappingJson, new TypeReference<Map<String, String>>() { });
        } catch (Exception e) {
            log.warn("rewardMappingJson is not a valid string-to-string JSON object: {}", e.getMessage());
            return Map.of();
        }
    }
}
