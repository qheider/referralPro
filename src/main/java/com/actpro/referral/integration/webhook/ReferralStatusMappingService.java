package com.actpro.referral.integration.webhook;

import com.actpro.referral.referral.ReferralStatus;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.EnumSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Parses {@code CompanyIntegration.statusMappingJson} (company-status-string -> ReferralStatus
 * name) and applies an explicit, out-of-order-safe transition policy - the single place the
 * "out-of-order status events follow an explicit transition policy" requirement is satisfied.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ReferralStatusMappingService {

    // Forward progression rank. Anything not present here (e.g. ACTIVE, LINK_OPENED) is treated
    // as rank 0 for the *current* side only - so a referral still in its initial state can always
    // move forward into REGISTERED and beyond.
    private static final Map<ReferralStatus, Integer> FORWARD_RANK = Map.of(
            ReferralStatus.REGISTERED, 1,
            ReferralStatus.BOOKING_STARTED, 2,
            ReferralStatus.BOOKING_CONFIRMED, 3,
            ReferralStatus.RENTAL_STARTED, 4,
            ReferralStatus.COMPLETED, 5,
            ReferralStatus.CONVERTED, 6
    );

    // Can override any non-terminal current state, even "backward" relative to rank.
    private static final Set<ReferralStatus> TERMINAL_OVERRIDES = EnumSet.of(ReferralStatus.CANCELLED, ReferralStatus.REJECTED);

    // A referral already in one of these accepts no further transitions at all.
    private static final Set<ReferralStatus> TERMINAL_STATES =
            EnumSet.of(ReferralStatus.CANCELLED, ReferralStatus.REJECTED, ReferralStatus.EXPIRED, ReferralStatus.CONVERTED);

    // EXPIRED is time-based (owned by CampaignExpirationWorker-adjacent logic), never a valid
    // webhook mapping target.
    private static final Set<ReferralStatus> INVALID_MAPPING_TARGETS = EnumSet.of(ReferralStatus.EXPIRED);

    private final ObjectMapper objectMapper;

    /**
     * Validates every value in a candidate statusMappingJson is a real, non-EXPIRED ReferralStatus
     * constant. Called by CompanyIntegrationService#updateConfig at config-save time. Returns the
     * first invalid value found, or empty if all valid (or the map itself is empty/absent).
     */
    public Optional<String> findFirstInvalidMappingValue(String statusMappingJson) {
        Map<String, String> mapping = parse(statusMappingJson);
        for (String value : mapping.values()) {
            if (!isValidMappingTarget(value)) {
                return Optional.of(value);
            }
        }
        return Optional.empty();
    }

    /** Looks up the configured ReferralStatus for a webhook's incoming status string. */
    public Optional<ReferralStatus> mapStatus(String statusMappingJson, String incomingStatus) {
        if (incomingStatus == null) {
            return Optional.empty();
        }
        Map<String, String> mapping = parse(statusMappingJson);
        String mappedValue = mapping.get(incomingStatus);
        if (mappedValue == null || !isValidMappingTarget(mappedValue)) {
            return Optional.empty();
        }
        return Optional.of(ReferralStatus.valueOf(mappedValue));
    }

    /**
     * Whether {@code mapped} should actually be applied on top of {@code current}. A referral
     * already in a terminal state accepts nothing further; CANCELLED/REJECTED can override any
     * other non-terminal state; otherwise only a strictly-forward rank move is allowed.
     */
    public boolean isTransitionAllowed(ReferralStatus current, ReferralStatus mapped) {
        if (TERMINAL_STATES.contains(current)) {
            return false;
        }
        if (TERMINAL_OVERRIDES.contains(mapped)) {
            return true;
        }
        int currentRank = FORWARD_RANK.getOrDefault(current, 0);
        int mappedRank = FORWARD_RANK.getOrDefault(mapped, 0);
        return mappedRank > currentRank;
    }

    private boolean isValidMappingTarget(String value) {
        try {
            ReferralStatus status = ReferralStatus.valueOf(value);
            return !INVALID_MAPPING_TARGETS.contains(status);
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    private Map<String, String> parse(String statusMappingJson) {
        if (statusMappingJson == null || statusMappingJson.isBlank()) {
            return Map.of();
        }
        try {
            return objectMapper.readValue(statusMappingJson, new TypeReference<Map<String, String>>() { });
        } catch (Exception e) {
            log.warn("statusMappingJson is not a valid string-to-string JSON object: {}", e.getMessage());
            return Map.of();
        }
    }
}
