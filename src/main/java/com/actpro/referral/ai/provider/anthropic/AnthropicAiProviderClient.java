package com.actpro.referral.ai.provider.anthropic;

import com.actpro.referral.ai.provider.AiProvider;
import com.actpro.referral.ai.provider.AiProviderClient;
import com.actpro.referral.ai.provider.ChatRequest;
import com.actpro.referral.ai.provider.ChatResult;
import com.actpro.referral.ai.provider.ConversationTurn;
import com.actpro.referral.ai.provider.ToolCall;
import com.actpro.referral.ai.provider.ToolOutcome;
import com.actpro.referral.ai.provider.ToolSpec;
import com.anthropic.client.AnthropicClient;
import com.anthropic.client.okhttp.AnthropicOkHttpClient;
import com.anthropic.core.JsonValue;
import com.anthropic.models.messages.ContentBlockParam;
import com.anthropic.models.messages.Message;
import com.anthropic.models.messages.MessageCreateParams;
import com.anthropic.models.messages.MessageParam;
import com.anthropic.models.messages.TextBlockParam;
import com.anthropic.models.messages.Tool;
import com.anthropic.models.messages.ToolResultBlockParam;
import com.anthropic.models.messages.ToolUseBlockParam;
import com.fasterxml.jackson.core.type.TypeReference;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * {@link AiProviderClient} backed by the Anthropic SDK. Owns client construction plus translation
 * between the neutral {@code com.actpro.referral.ai.provider} types and Anthropic's message/tool
 * wire format - {@code DashboardCopilotService} never sees an Anthropic type directly.
 */
@Component
@Slf4j
public class AnthropicAiProviderClient implements AiProviderClient {

    @Value("${app.ai.anthropic.api-key:}")
    private String apiKey;

    @Value("${app.ai.anthropic.model:claude-sonnet-5}")
    private String model;

    private AnthropicClient client;

    @PostConstruct
    void init() {
        if (apiKey != null && !apiKey.isBlank()) {
            client = AnthropicOkHttpClient.builder().apiKey(apiKey).build();
        } else {
            log.debug("app.ai.anthropic.api-key is not set - the Anthropic provider is unavailable until configured");
        }
    }

    @Override
    public AiProvider provider() {
        return AiProvider.ANTHROPIC;
    }

    @Override
    public boolean isConfigured() {
        return client != null;
    }

    @Override
    public ChatResult chat(ChatRequest request) {
        if (client == null) {
            throw new IllegalStateException("Anthropic client is not configured (app.ai.anthropic.api-key is blank)");
        }

        MessageCreateParams.Builder paramsBuilder = MessageCreateParams.builder()
                .model(model)
                .maxTokens(request.maxTokens())
                .system(request.systemPrompt());
        for (ToolSpec tool : request.tools()) {
            paramsBuilder.addTool(toAnthropicTool(tool));
        }
        List<MessageParam> messages = new ArrayList<>();
        for (ConversationTurn turn : request.turns()) {
            messages.add(toMessageParam(turn));
        }
        paramsBuilder.messages(messages);

        Message response = client.messages().create(paramsBuilder.build());
        return toChatResult(response);
    }

    @SuppressWarnings("unchecked")
    private Tool toAnthropicTool(ToolSpec spec) {
        Map<String, Object> schema = spec.parametersSchema();
        Tool.InputSchema.Properties.Builder propertiesBuilder = Tool.InputSchema.Properties.builder();
        Object properties = schema.get("properties");
        if (properties instanceof Map<?, ?> propertiesMap) {
            for (var entry : propertiesMap.entrySet()) {
                propertiesBuilder.putAdditionalProperty((String) entry.getKey(), JsonValue.from(entry.getValue()));
            }
        }
        Tool.InputSchema.Builder schemaBuilder = Tool.InputSchema.builder().properties(propertiesBuilder.build());
        Object required = schema.get("required");
        if (required instanceof List<?> requiredList) {
            schemaBuilder.required((List<String>) requiredList);
        }
        return Tool.builder()
                .name(spec.name())
                .description(spec.description())
                .inputSchema(schemaBuilder.build())
                .build();
    }

    private MessageParam toMessageParam(ConversationTurn turn) {
        return switch (turn) {
            case ConversationTurn.UserTurn userTurn ->
                    MessageParam.builder().role(MessageParam.Role.USER).content(userTurn.text()).build();
            case ConversationTurn.AssistantTurn assistantTurn -> {
                List<ContentBlockParam> blocks = new ArrayList<>();
                if (assistantTurn.text() != null && !assistantTurn.text().isBlank()) {
                    blocks.add(ContentBlockParam.ofText(TextBlockParam.builder().text(assistantTurn.text()).build()));
                }
                for (ToolCall toolCall : assistantTurn.toolCalls()) {
                    ToolUseBlockParam.Input.Builder inputBuilder = ToolUseBlockParam.Input.builder();
                    for (var entry : toolCall.arguments().entrySet()) {
                        inputBuilder.putAdditionalProperty(entry.getKey(), JsonValue.from(entry.getValue()));
                    }
                    blocks.add(ContentBlockParam.ofToolUse(ToolUseBlockParam.builder()
                            .id(toolCall.id())
                            .name(toolCall.name())
                            .input(inputBuilder.build())
                            .build()));
                }
                yield MessageParam.builder().role(MessageParam.Role.ASSISTANT).contentOfBlockParams(blocks).build();
            }
            case ConversationTurn.ToolResultTurn toolResultTurn -> {
                List<ContentBlockParam> blocks = new ArrayList<>();
                for (ToolOutcome outcome : toolResultTurn.outcomes()) {
                    blocks.add(ContentBlockParam.ofToolResult(ToolResultBlockParam.builder()
                            .toolUseId(outcome.toolCallId())
                            .content(outcome.content())
                            .isError(outcome.isError())
                            .build()));
                }
                yield MessageParam.builder().role(MessageParam.Role.USER).contentOfBlockParams(blocks).build();
            }
        };
    }

    private ChatResult toChatResult(Message response) {
        StringBuilder text = new StringBuilder();
        List<ToolCall> toolCalls = new ArrayList<>();
        for (var block : response.content()) {
            block.text().ifPresent(t -> {
                if (!text.isEmpty()) {
                    text.append("\n");
                }
                text.append(t.text());
            });
            block.toolUse().ifPresent(toolUse -> toolCalls.add(new ToolCall(
                    toolUse.id(),
                    toolUse.name(),
                    toolUse._input().convert(new TypeReference<Map<String, Object>>() {
                    }))));
        }
        return new ChatResult(text.toString(), toolCalls);
    }
}
