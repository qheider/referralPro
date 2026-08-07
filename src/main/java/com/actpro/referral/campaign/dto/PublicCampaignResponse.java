package com.actpro.referral.campaign.dto;

/**
 * Response for the public, unauthenticated GET /api/campaigns/join/{campaignCode} link
 * resolution - a prospective ambassador opening the published campaign link. Deliberately
 * excludes internal ids, reward terms, and anything else not meant for a public visitor;
 * companyId is included only because it's the existing public apply flow's required parameter
 * (POST /api/ambassador-applications/apply?companyId=...), not a general-purpose id leak.
 */
public record PublicCampaignResponse(
        String campaignCode,
        Long companyId,
        String companyName,
        String campaignName,
        String description,
        boolean enrollmentOpen,
        String unavailableReason
) {
}
