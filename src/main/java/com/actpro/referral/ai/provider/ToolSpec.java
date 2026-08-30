package com.actpro.referral.ai.provider;

import java.util.Map;

/**
 * A provider-agnostic tool/function declaration. {@code parametersSchema} is a plain JSON Schema
 * object (e.g. {@code {"type": "object", "properties": {...}, "required": [...]}}) - both the
 * Anthropic and OpenAI tool-calling APIs accept this shape natively, so no per-provider schema
 * translation is needed here; each {@link AiProviderClient} just wires it into its own request
 * format.
 */
public record ToolSpec(String name, String description, Map<String, Object> parametersSchema) {
}
