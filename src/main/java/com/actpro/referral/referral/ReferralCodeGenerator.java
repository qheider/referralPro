package com.actpro.referral.referral;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.security.SecureRandom;

@Component
@RequiredArgsConstructor
public class ReferralCodeGenerator {

    private static final String CHARACTERS = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
    private static final int CODE_LENGTH = 8;
    private static final SecureRandom random = new SecureRandom();

    private final ReferralRepository referralRepository;

    public String generateUniqueCode() {
        String code;
        int attempts = 0;
        int maxAttempts = 10;

        do {
            code = generateCode();
            attempts++;
            if (attempts > maxAttempts) {
                throw new RuntimeException("Failed to generate unique referral code after " + maxAttempts + " attempts");
            }
        } while (referralRepository.existsByReferralCode(code));

        return code;
    }

    private String generateCode() {
        StringBuilder code = new StringBuilder(CODE_LENGTH);
        for (int i = 0; i < CODE_LENGTH; i++) {
            code.append(CHARACTERS.charAt(random.nextInt(CHARACTERS.length())));
        }
        return code.toString();
    }
}
