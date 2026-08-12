package com.actpro.referral.integration.webhook;

import com.actpro.referral.company.CompanyIntegrationRepository;
import org.springframework.stereotype.Component;

import java.security.SecureRandom;

/**
 * Generates the public, non-sequential webhookPublicId used to resolve a company from an inbound
 * webhook URL (/api/v1/integrations/{webhookPublicId}/webhooks/service-status) - mirrors
 * {@code campaign.CampaignCodeGenerator}'s approach (never expose the database primary key).
 */
@Component
public class WebhookPublicIdGenerator {

    private static final String CODE_ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
    private static final int CODE_LENGTH = 16;
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final CompanyIntegrationRepository companyIntegrationRepository;

    public WebhookPublicIdGenerator(CompanyIntegrationRepository companyIntegrationRepository) {
        this.companyIntegrationRepository = companyIntegrationRepository;
    }

    public String generateUniqueId() {
        String id;
        do {
            id = randomString(CODE_LENGTH);
        } while (companyIntegrationRepository.existsByWebhookPublicId(id));

        return id;
    }

    private String randomString(int length) {
        StringBuilder builder = new StringBuilder(length);
        for (int index = 0; index < length; index++) {
            builder.append(CODE_ALPHABET.charAt(SECURE_RANDOM.nextInt(CODE_ALPHABET.length())));
        }
        return builder.toString();
    }
}
