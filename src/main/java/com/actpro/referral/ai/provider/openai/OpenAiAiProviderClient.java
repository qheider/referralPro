package com.actpro.referral.ai.provider.openai;

import com.actpro.referral.ai.provider.AiProvider;
import com.actpro.referral.ai.provider.AiProviderClient;
import com.actpro.referral.ai.provider.ChatRequest;
import com.actpro.referral.ai.provider.ChatResult;
import com.actpro.referral.ai.provider.ConversationTurn;
import com.actpro.referral.ai.provider.ToolCall;
import com.actpro.referral.ai.provider.ToolOutcome;
import com.actpro.referral.ai.provider.ToolSpec;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * {@link AiProviderClient} backed by OpenAI's Chat Completions API. No OpenAI SDK dependency -
 * calls the REST endpoint directly with Spring's {@code RestClient} and Jackson (both already on
 * the classpath), which keeps this provider a drop-in sibling of the Anthropic SDK-backed one
 * without adding a second HTTP client SDK to the project.
 */
@Component
@Slf4j
public class OpenAiAiProviderClient implements AiProviderClient {

    private static final String BASE_URL = "https://api.openai.com/v1";

    private final ObjectMapper objectMapper;

    @Value("${app.ai.openai.api-key:}")
    private String apiKey;

    @Value("${app.ai.openai.model:gpt-4o}")
    private String model;

    private RestClient restClient;

    public OpenAiAiProviderClient(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @PostConstruct
    void init() {
        if (apiKey != null && !apiKey.isBlank()) {
            restClient = RestClient.builder()
                    .baseUrl(BASE_URL)
                    .defaultHeader("Authorization", "Bearer " + apiKey)
                    .build();
        } else {
            log.debug("app.ai.openai.api-key is not set - the OpenAI provider is unavailable until configured");
        }
    }

    @Override
    public AiProvider provider() {
        return AiProvider.OPENAI;
    }

    @Override
    public boolean isConfigured() {
        return restClient != null;
    }

    @Override
    public ChatResult chat(ChatRequest request) {
        if (restClient == null) {
            throw new IllegalStateException("OpenAI client is not configured (app.ai.openai.api-key is blank)");
        }

        ObjectNode body = objectMapper.createObjectNode();
        body.put("model", model);
        body.put("max_completion_tokens", request.maxTokens());
        body.set("messages", toMessagesArray(request));
        if (!request.tools().isEmpty()) {
            body.set("tools", toToolsArray(request.tools()));
        }

        JsonNode response;
        try {
            response = restClient.post()
                    .uri("/chat/completions")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .body(JsonNode.class);
        } catch (RestClientResponseException e) {
            throw new IllegalStateException(
                    "OpenAI request failed (" + e.getStatusCode() + "): " + e.getResponseBodyAsString(), e);
        }

        return toChatResult(response);
    }

    private ArrayNode toMessagesArray(ChatRequest request) {
        ArrayNode messages = objectMapper.createArrayNode();
        ObjectNode system = objectMapper.createObjectNode();
        system.put("role", "system");
        system.put("content", request.systemPrompt());
        messages.add(system);
        for (ConversationTurn turn : request.turns()) {
            appendTurn(messages, turn);
        }
        return messages;
    }

    private void appendTurn(ArrayNode messages, ConversationTurn turn) {
        switch (turn) {
            case ConversationTurn.UserTurn userTurn -> {
                ObjectNode node = objectMapper.createObjectNode();
                node.put("role", "user");
                node.put("content", userTurn.text());
                messages.add(node);
            }
            case ConversationTurn.AssistantTurn assistantTurn -> messages.add(toAssistantMessageNode(assistantTurn));
            case ConversationTurn.ToolResultTurn toolResultTurn -> {
                for (ToolOutcome outcome : toolResultTurn.outcomes()) {
                    ObjectNode node = objectMapper.createObjectNode();
                    node.put("role", "tool");
                    node.put("tool_call_id", outcome.toolCallId());
                    node.put("content", outcome.content());
                    messages.add(node);
                }
            }
        }
    }

    private ObjectNode toAssistantMessageNode(ConversationTurn.AssistantTurn assistantTurn) {
        ObjectNode node = objectMapper.createObjectNode();
        node.put("role", "assistant");
        if (assistantTurn.text() != null && !assistantTurn.text().isBlank()) {
            node.put("content", assistantTurn.text());
        } else {
            node.putNull("content");
        }
        if (!assistantTurn.toolCalls().isEmpty()) {
            ArrayNode toolCalls = objectMapper.createArrayNode();
            for (ToolCall toolCall : assistantTurn.toolCalls()) {
                ObjectNode toolCallNode = objectMapper.createObjectNode();
                toolCallNode.put("id", toolCall.id());
                toolCallNode.put("type", "function");
                ObjectNode function = objectMapper.createObjectNode();
                function.put("name", toolCall.name());
                function.put("arguments", writeJson(toolCall.arguments()));
                toolCallNode.set("function", function);
                toolCalls.add(toolCallNode);
            }
            node.set("tool_calls", toolCalls);
        }
        return node;
    }

    private ArrayNode toToolsArray(List<ToolSpec> tools) {
        ArrayNode array = objectMapper.createArrayNode();
        for (ToolSpec tool : tools) {
            ObjectNode node = objectMapper.createObjectNode();
            node.put("type", "function");
            ObjectNode function = objectMapper.createObjectNode();
            function.put("name", tool.name());
            function.put("description", tool.description());
            function.set("parameters", objectMapper.valueToTree(tool.parametersSchema()));
            node.set("function", function);
            array.add(node);
        }
        return array;
    }

    private ChatResult toChatResult(JsonNode response) {
        if (response == null || !response.hasNonNull("choices") || response.get("choices").isEmpty()) {
            throw new IllegalStateException("OpenAI response contained no choices: " + response);
        }
        JsonNode message = response.get("choices").get(0).get("message");
        String text = message.hasNonNull("content") ? message.get("content").asText() : "";

        List<ToolCall> toolCalls = new ArrayList<>();
        if (message.hasNonNull("tool_calls")) {
            for (JsonNode toolCallNode : message.get("tool_calls")) {
                JsonNode function = toolCallNode.get("function");
                toolCalls.add(new ToolCall(
                        toolCallNode.get("id").asText(),
                        function.get("name").asText(),
                        readArguments(function.get("arguments").asText())));
            }
        }
        return new ChatResult(text, toolCalls);
    }

    private String writeJson(Map<String, Object> value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to serialize tool call arguments", e);
        }
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> readArguments(String json) {
        try {
            return objectMapper.readValue(json, Map.class);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to parse OpenAI tool call arguments: " + json, e);
        }
    }
}
