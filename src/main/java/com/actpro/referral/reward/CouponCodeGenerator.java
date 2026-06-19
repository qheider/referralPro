package com.actpro.referral.reward;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.security.SecureRandom;

@Component
@RequiredArgsConstructor
public class CouponCodeGenerator {

    private static final String CHARACTERS = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
    private static final int CODE_LENGTH = 8;
    private static final String PREFIX = "REF-";
    private static final SecureRandom random = new SecureRandom();

    private final RewardRepository rewardRepository;

    public String generate() {
        String code;
        int attempts = 0;
        int maxAttempts = 10;

        do {
            code = generateCode();
            attempts++;
            if (attempts > maxAttempts) {
                throw new RuntimeException("Failed to generate unique coupon code after " + maxAttempts + " attempts");
            }
        } while (rewardRepository.existsByCouponCode(code));

        return code;
    }

    private String generateCode() {
        StringBuilder code = new StringBuilder(PREFIX);
        for (int i = 0; i < CODE_LENGTH; i++) {
            code.append(CHARACTERS.charAt(random.nextInt(CHARACTERS.length())));
        }
        return code.toString();
    }
}
