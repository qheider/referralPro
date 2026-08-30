package com.actpro.referral.ai.provider;

import java.util.List;

/**
 * The model's reply to one {@link ChatRequest}. {@code toolCalls} is empty when the model produced
 * a final answer instead of asking for tool dispatch - callers should treat a non-empty list as
 * "loop again after dispatching these", matching {@code StopReason.TOOL_USE} in the old
 * Anthropic-only code.
 */
public record ChatResult(String text, List<ToolCall> toolCalls) {

    public boolean hasToolCalls() {
        return toolCalls != null && !toolCalls.isEmpty();
    }
}
