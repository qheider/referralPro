package com.actpro.referral.revenue;

import com.actpro.referral.referral.ReferralStatus;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;

class RewardStatusMappingServiceTest {

    private final RewardStatusMappingService service = new RewardStatusMappingService(new ObjectMapper());

    @Test
    void shouldDefaultCompletedAndConvertedToQualifying() {
        assertEquals(RewardMappingClassification.QUALIFYING, service.classify(null, ReferralStatus.COMPLETED));
        assertEquals(RewardMappingClassification.QUALIFYING, service.classify("", ReferralStatus.CONVERTED));
    }

    @Test
    void shouldDefaultCancelledAndRejectedToReversing() {
        assertEquals(RewardMappingClassification.REVERSING, service.classify(null, ReferralStatus.CANCELLED));
        assertEquals(RewardMappingClassification.REVERSING, service.classify(null, ReferralStatus.REJECTED));
    }

    @Test
    void shouldDefaultEverythingElseToIgnore() {
        assertEquals(RewardMappingClassification.IGNORE, service.classify(null, ReferralStatus.BOOKING_STARTED));
        assertEquals(RewardMappingClassification.IGNORE, service.classify(null, ReferralStatus.REGISTERED));
        assertEquals(RewardMappingClassification.IGNORE, service.classify(null, ReferralStatus.EXPIRED));
    }

    @Test
    void shouldAllowCompanyOverrideOfClassification() {
        String json = "{\"BOOKING_CONFIRMED\":\"QUALIFYING\",\"COMPLETED\":\"IGNORE\"}";

        assertEquals(RewardMappingClassification.QUALIFYING, service.classify(json, ReferralStatus.BOOKING_CONFIRMED));
        assertEquals(RewardMappingClassification.IGNORE, service.classify(json, ReferralStatus.COMPLETED));
        // Unmapped entries still fall back to the default.
        assertEquals(RewardMappingClassification.REVERSING, service.classify(json, ReferralStatus.CANCELLED));
    }

    @Test
    void shouldFallBackToDefaultWhenOverrideValueIsInvalid() {
        String json = "{\"COMPLETED\":\"NOT_A_REAL_CLASSIFICATION\"}";

        assertEquals(RewardMappingClassification.QUALIFYING, service.classify(json, ReferralStatus.COMPLETED));
    }

    @Test
    void shouldDetectInvalidStatusKey() {
        Optional<String> invalid = service.findFirstInvalidMappingEntry("{\"NOT_A_STATUS\":\"QUALIFYING\"}");

        assertEquals(Optional.of("NOT_A_STATUS"), invalid);
    }

    @Test
    void shouldDetectInvalidClassificationValue() {
        Optional<String> invalid = service.findFirstInvalidMappingEntry("{\"COMPLETED\":\"NOT_VALID\"}");

        assertEquals(Optional.of("NOT_VALID"), invalid);
    }

    @Test
    void shouldAcceptAllValidEntries() {
        String json = "{\"BOOKING_CONFIRMED\":\"QUALIFYING\",\"CANCELLED\":\"REVERSING\",\"REGISTERED\":\"IGNORE\"}";

        assertEquals(Optional.empty(), service.findFirstInvalidMappingEntry(json));
    }

    @Test
    void shouldTreatBlankOrNullJsonAsNoOverrides() {
        assertEquals(Optional.empty(), service.findFirstInvalidMappingEntry(null));
        assertEquals(Optional.empty(), service.findFirstInvalidMappingEntry(""));
    }
}
