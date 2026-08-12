package com.actpro.referral.integration.webhook;

import com.actpro.referral.referral.ReferralStatus;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ReferralStatusMappingServiceTest {

    private final ReferralStatusMappingService service = new ReferralStatusMappingService(new ObjectMapper());

    @Test
    void shouldMapKnownStatusString() {
        String json = "{\"SERVICE_COMPLETED\":\"COMPLETED\"}";

        Optional<ReferralStatus> result = service.mapStatus(json, "SERVICE_COMPLETED");

        assertEquals(Optional.of(ReferralStatus.COMPLETED), result);
    }

    @Test
    void shouldReturnEmptyForUnmappedStatusString() {
        String json = "{\"SERVICE_COMPLETED\":\"COMPLETED\"}";

        assertEquals(Optional.empty(), service.mapStatus(json, "SOMETHING_ELSE"));
    }

    @Test
    void shouldReturnEmptyWhenNoMappingConfigured() {
        assertEquals(Optional.empty(), service.mapStatus(null, "SERVICE_COMPLETED"));
        assertEquals(Optional.empty(), service.mapStatus("", "SERVICE_COMPLETED"));
    }

    @Test
    void shouldDetectMappingValueThatIsNotARealEnumConstant() {
        String json = "{\"SERVICE_COMPLETED\":\"NOT_A_REAL_STATUS\"}";

        Optional<String> invalid = service.findFirstInvalidMappingValue(json);

        assertEquals(Optional.of("NOT_A_REAL_STATUS"), invalid);
    }

    @Test
    void shouldRejectExpiredAsAMappingTarget() {
        String json = "{\"SERVICE_CANCELLED\":\"EXPIRED\"}";

        assertEquals(Optional.of("EXPIRED"), service.findFirstInvalidMappingValue(json));
        assertEquals(Optional.empty(), service.mapStatus(json, "SERVICE_CANCELLED"));
    }

    @Test
    void shouldAcceptAllValidMappingValues() {
        String json = "{\"A\":\"BOOKING_STARTED\",\"B\":\"COMPLETED\",\"C\":\"CANCELLED\"}";

        assertEquals(Optional.empty(), service.findFirstInvalidMappingValue(json));
    }

    @Test
    void shouldAllowForwardTransition() {
        assertTrue(service.isTransitionAllowed(ReferralStatus.REGISTERED, ReferralStatus.BOOKING_STARTED));
        assertTrue(service.isTransitionAllowed(ReferralStatus.BOOKING_CONFIRMED, ReferralStatus.RENTAL_STARTED));
    }

    @Test
    void shouldRejectBackwardTransition() {
        assertFalse(service.isTransitionAllowed(ReferralStatus.COMPLETED, ReferralStatus.BOOKING_STARTED));
        assertFalse(service.isTransitionAllowed(ReferralStatus.RENTAL_STARTED, ReferralStatus.REGISTERED));
    }

    @Test
    void shouldAllowTerminalOverrideFromNonTerminalState() {
        assertTrue(service.isTransitionAllowed(ReferralStatus.RENTAL_STARTED, ReferralStatus.CANCELLED));
        assertTrue(service.isTransitionAllowed(ReferralStatus.REGISTERED, ReferralStatus.REJECTED));
    }

    @Test
    void shouldRejectAnyTransitionWhenReferralAlreadyTerminal() {
        assertFalse(service.isTransitionAllowed(ReferralStatus.CONVERTED, ReferralStatus.COMPLETED));
        assertFalse(service.isTransitionAllowed(ReferralStatus.CANCELLED, ReferralStatus.CANCELLED));
        assertFalse(service.isTransitionAllowed(ReferralStatus.REJECTED, ReferralStatus.BOOKING_STARTED));
        assertFalse(service.isTransitionAllowed(ReferralStatus.EXPIRED, ReferralStatus.COMPLETED));
    }

    @Test
    void shouldAllowRegisteredFromInitialActiveState() {
        assertTrue(service.isTransitionAllowed(ReferralStatus.ACTIVE, ReferralStatus.REGISTERED));
        assertTrue(service.isTransitionAllowed(ReferralStatus.LINK_OPENED, ReferralStatus.REGISTERED));
    }
}
