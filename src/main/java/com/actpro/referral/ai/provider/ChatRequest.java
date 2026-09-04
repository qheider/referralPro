package com.actpro.referral.ai.provider;

import java.util.List;

/**
 * A single turn of the agentic loop, sent to whichever {@link AiProviderClient} is configured.
 * Model choice and API key live on the provider client itself (per-provider config under
 * {@code app.ai.<provider>.*}), not here - a request is otherwise identical regardless of which
 * provider ends up serving it.
 */
public record ChatRequest(String systemPrompt, List<ConversationTurn> turns, List<ToolSpec> tools, long maxTokens) {
}
