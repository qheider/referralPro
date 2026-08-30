package com.actpro.referral.ai;

import com.actpro.referral.ai.provider.AiProvider;
import com.actpro.referral.ai.provider.AiProviderClient;
import com.actpro.referral.ai.provider.ChatRequest;
import com.actpro.referral.ai.provider.ChatResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Picks the {@link AiProviderClient} named by {@code app.ai.provider} (currently {@code anthropic}
 * or {@code openai}) and delegates to it. {@link DashboardCopilotService} talks to this class -
 * and only to the provider-neutral types in {@code com.actpro.referral.ai.provider} - so switching
 * providers, or adding a new one, is a config change plus one new {@link AiProviderClient} bean,
 * never a change here or in DashboardCopilotService.
 */
@Component
@Slf4j
public class AiClient {

    private final Map<AiProvider, AiProviderClient> clientsByProvider;

    @Value("${app.ai.provider:anthropic}")
    private String providerName;

    public AiClient(List<AiProviderClient> providerClients) {
        this.clientsByProvider = providerClients.stream()
                .collect(Collectors.toMap(AiProviderClient::provider, Function.identity()));
    }

    public boolean isConfigured() {
        return resolve().map(AiProviderClient::isConfigured).orElse(false);
    }

    public ChatResult chat(ChatRequest request) {
        AiProviderClient client = resolve()
                .orElseThrow(() -> new IllegalStateException("No AI provider client registered for '" + providerName + "'"));
        return client.chat(request);
    }

    private Optional<AiProviderClient> resolve() {
        String name = providerName == null ? "" : providerName.trim().toUpperCase();
        try {
            return Optional.ofNullable(clientsByProvider.get(AiProvider.valueOf(name)));
        } catch (IllegalArgumentException e) {
            log.warn("Unknown app.ai.provider '{}' - expected one of {}", providerName, AiProvider.values());
            return Optional.empty();
        }
    }
}
