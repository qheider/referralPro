package com.actpro.referral.referral;

import org.springframework.stereotype.Component;

import java.security.SecureRandom;

@Component
public class ReferralTokenGenerator {

    private static final String TOKEN_ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz23456789";
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final ReferralLinkRepository referralLinkRepository;

    public ReferralTokenGenerator(ReferralLinkRepository referralLinkRepository) {
        this.referralLinkRepository = referralLinkRepository;
    }

    public String generateUniqueToken() {
        String token;
        do {
            token = randomString(16);
        } while (referralLinkRepository.existsByPublicToken(token));

        return token;
    }

    private String randomString(int length) {
        StringBuilder builder = new StringBuilder(length);
        for (int index = 0; index < length; index++) {
            builder.append(TOKEN_ALPHABET.charAt(SECURE_RANDOM.nextInt(TOKEN_ALPHABET.length())));
        }
        return builder.toString();
    }
}
