package com.actpro.referral.ai.provider;

import java.util.List;

/**
 * One turn of a conversation, kept provider-neutral so {@code DashboardCopilotService} never
 * touches an Anthropic- or OpenAI-specific message type directly. Each {@link AiProviderClient}
 * translates a {@link ChatRequest}'s turn list into its own wire format on the way out, and
 * translates its own response back into an {@link AssistantTurn} on the way in.
 */
public sealed interface ConversationTurn permits ConversationTurn.UserTurn, ConversationTurn.AssistantTurn, ConversationTurn.ToolResultTurn {

    record UserTurn(String text) implements ConversationTurn {
    }

    record AssistantTurn(String text, List<ToolCall> toolCalls) implements ConversationTurn {
    }

    record ToolResultTurn(List<ToolOutcome> outcomes) implements ConversationTurn {
    }
}
