package com.actpro.referral.ai.provider;

/**
 * A single LLM backend capable of driving the dashboard copilot's tool-calling loop. Implementations
 * own their own SDK/HTTP client, API key, and model config (under {@code app.ai.<provider>.*}) and
 * are selected at runtime by {@code app.ai.provider} - see {@code AiClient}. Adding a new provider
 * (e.g. a third option beyond Anthropic/OpenAI) means adding an {@link AiProvider} value and one
 * more implementation of this interface; nothing in {@code DashboardCopilotService} or
 * {@code DashboardToolDefinitions} needs to change.
 */
public interface AiProviderClient {

    AiProvider provider();

    /**
     * True once this client has everything it needs to call its provider (i.e. its API key is
     * set). Does not guarantee the credentials are valid - only that a call will be attempted.
     */
    boolean isConfigured();

    ChatResult chat(ChatRequest request);
}
