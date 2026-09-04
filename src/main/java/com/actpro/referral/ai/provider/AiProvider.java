package com.actpro.referral.ai.provider;

/**
 * The set of LLM providers {@link AiProviderClient} implementations exist for. Selected at
 * runtime via {@code app.ai.provider} - adding a new provider means adding an enum value plus
 * an {@link AiProviderClient} bean that returns it from {@link AiProviderClient#provider()}, no
 * other code changes.
 */
public enum AiProvider {
    ANTHROPIC,
    OPENAI
}
