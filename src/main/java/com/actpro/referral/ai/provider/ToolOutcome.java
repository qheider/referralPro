package com.actpro.referral.ai.provider;

/**
 * The result of dispatching one {@link ToolCall}. {@code content} is a plain string (the caller
 * is expected to JSON-serialize structured results itself) so this type stays provider-neutral.
 */
public record ToolOutcome(String toolCallId, String content, boolean isError) {
}
