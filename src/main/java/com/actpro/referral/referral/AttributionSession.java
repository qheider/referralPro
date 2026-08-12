package com.actpro.referral.referral;

import java.util.regex.Pattern;

/**
 * Shared constants for the rp_attr_session cookie used to correlate a {@code GET /r/{token}}
 * click ({@link ReferralRedirectController}) with a later public lead submission against the
 * same referral link ({@code ReferralLeadController}). Both controllers must agree on the exact
 * cookie name for that correlation to work, so it lives here once rather than as two
 * independently-maintained private constants.
 */
public final class AttributionSession {

    public static final String COOKIE_NAME = "rp_attr_session";

    // Matches only the values this system itself generates (UUID) - anything else from the
    // client is untrusted and is not reused as-is (also enforces the
    // Referral/ReferralClick sessionId VARCHAR(100) column limit so an oversized or tampered
    // cookie can't fail an insert).
    private static final Pattern VALID_SESSION_ID = Pattern.compile("^[A-Za-z0-9-]{1,100}$");

    private AttributionSession() {
    }

    public static boolean isValid(String sessionId) {
        return sessionId != null && VALID_SESSION_ID.matcher(sessionId).matches();
    }
}
