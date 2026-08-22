package com.actpro.referral.referral.dto;

import com.actpro.referral.referral.ReferralStatus;

import java.time.LocalDateTime;

public record SubmitReferralLeadResponse(
        String referralCode,
        ReferralStatus status,
        LocalDateTime registeredAt,
        // The referral link's company-configured destinationUrl (with ?ref={referralCode}
        // appended), only once the lead has been registered by ReferralPro - null when the link
        // has no destinationUrl, in which case the caller (campaign-refer.component) just shows
        // its own confirmation. See ReferralClickService for why this is deferred to here instead
        // of used as the click-time redirect.
        String redirectUrl
) {
}
