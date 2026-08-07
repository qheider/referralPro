package com.actpro.referral.campaign;

import org.springframework.stereotype.Component;

import java.security.SecureRandom;

/**
 * Generates the public, non-sequential campaign_code used in the /join/{campaignCode} enrollment
 * link - mirrors ReferralTokenGenerator's approach (never expose the database primary key).
 */
@Component
public class CampaignCodeGenerator {

    private static final String CODE_ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
    private static final int CODE_LENGTH = 10;
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final CampaignRepository campaignRepository;

    public CampaignCodeGenerator(CampaignRepository campaignRepository) {
        this.campaignRepository = campaignRepository;
    }

    public String generateUniqueCode() {
        String code;
        do {
            code = randomString(CODE_LENGTH);
        } while (campaignRepository.existsByCampaignCode(code));

        return code;
    }

    private String randomString(int length) {
        StringBuilder builder = new StringBuilder(length);
        for (int index = 0; index < length; index++) {
            builder.append(CODE_ALPHABET.charAt(SECURE_RANDOM.nextInt(CODE_ALPHABET.length())));
        }
        return builder.toString();
    }
}
