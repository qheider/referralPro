package com.actpro.referral.ai.provider;

import java.util.Map;

/**
 * A tool invocation requested by the model - {@code id} is the provider-issued call id that must
 * be echoed back in the matching {@link ToolOutcome} so the provider can pair the result to the
 * request.
 */
public record ToolCall(String id, String name, Map<String, Object> arguments) {
}
