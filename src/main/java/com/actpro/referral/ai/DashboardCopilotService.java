package com.actpro.referral.ai;

import com.actpro.referral.ai.dto.AskDashboardResponse;
import com.actpro.referral.ai.dto.ReferencedCampaign;
import com.actpro.referral.ai.provider.ChatRequest;
import com.actpro.referral.ai.provider.ChatResult;
import com.actpro.referral.ai.provider.ConversationTurn;
import com.actpro.referral.ai.provider.ToolCall;
import com.actpro.referral.ai.provider.ToolOutcome;
import com.actpro.referral.ai.provider.ToolSpec;
import com.actpro.referral.common.exception.BadRequestException;
import com.actpro.referral.dashboard.DashboardService;
import com.actpro.referral.dashboard.dto.CampaignOverviewItem;
import com.actpro.referral.dashboard.dto.CampaignStatsResponse;
import com.actpro.referral.dashboard.dto.CampaignsOverviewResponse;
import com.actpro.referral.dashboard.dto.ConversionFunnelResponse;
import com.actpro.referral.dashboard.dto.RewardSummaryResponse;
import com.actpro.referral.dashboard.dto.TimeSeriesResponse;
import com.actpro.referral.dashboard.dto.TopReferrersResponse;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Orchestrates the "ask your dashboard" agentic loop: sends the admin's question plus the
 * DashboardService-backed tool schemas to whichever LLM provider is configured (see
 * {@link AiClient}), dispatches any tool calls straight to DashboardService (which is itself
 * tenant-scoped via CurrentUserService - see DashboardToolDefinitions for why no tool ever takes a
 * companyId), and loops until the model produces a final natural-language answer or the iteration
 * cap is hit. Everything here is provider-neutral - see {@code com.actpro.referral.ai.provider}.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class DashboardCopilotService {

    // Short, synthesis-only answers - the loop is intentionally cheap per turn.
    private static final long MAX_TOKENS_PER_TURN = 4096L;

    private final AiClient aiClient;
    private final DashboardToolDefinitions toolDefinitions;
    private final DashboardService dashboardService;
    private final ObjectMapper objectMapper;

    @Value("${app.ai.enabled:true}")
    private boolean enabled;

    @Value("${app.ai.max-tool-iterations:6}")
    private int maxToolIterations;

    public AskDashboardResponse ask(String question, Long campaignId) {
        if (!enabled || !aiClient.isConfigured()) {
            throw new BadRequestException("The AI dashboard copilot is not currently available");
        }

        List<ToolSpec> tools = toolDefinitions.all();
        String systemPrompt = buildSystemPrompt(campaignId);

        List<ConversationTurn> turns = new ArrayList<>();
        turns.add(new ConversationTurn.UserTurn(question));

        List<String> toolsUsed = new ArrayList<>();
        Map<Long, String> referencedCampaigns = new LinkedHashMap<>();

        ChatResult lastResult = null;
        for (int iteration = 0; iteration < maxToolIterations; iteration++) {
            log.debug("Dashboard copilot iteration {}: sending {} turns via {}", iteration, turns.size(), aiClient);
            lastResult = aiClient.chat(new ChatRequest(systemPrompt, turns, tools, MAX_TOKENS_PER_TURN));
            turns.add(new ConversationTurn.AssistantTurn(lastResult.text(), lastResult.toolCalls()));

            if (!lastResult.hasToolCalls()) {
                return buildResponse(lastResult, toolsUsed, referencedCampaigns);
            }

            List<ToolOutcome> outcomes = new ArrayList<>();
            for (ToolCall toolCall : lastResult.toolCalls()) {
                toolsUsed.add(toolCall.name());
                outcomes.add(dispatch(toolCall, referencedCampaigns));
            }
            turns.add(new ConversationTurn.ToolResultTurn(outcomes));
        }

        log.warn("Dashboard copilot hit the {}-iteration tool-call cap without a final answer", maxToolIterations);
        String fallback = lastResult == null ? "" : lastResult.text();
        String answer = fallback == null || fallback.isBlank()
                ? "I wasn't able to fully answer that within the allowed number of steps. Try asking a more specific question."
                : fallback;
        return new AskDashboardResponse(answer, toolsUsed, toReferencedCampaignList(referencedCampaigns));
    }

    private ToolOutcome dispatch(ToolCall toolCall, Map<Long, String> referencedCampaigns) {
        try {
            Object result = invokeTool(toolCall, referencedCampaigns);
            return new ToolOutcome(toolCall.id(), toJson(result), false);
        } catch (RuntimeException e) {
            log.debug("Dashboard copilot tool call {} failed: {}", toolCall.name(), e.getMessage());
            return new ToolOutcome(toolCall.id(), "Error: " + e.getMessage(), true);
        }
    }

    private Object invokeTool(ToolCall toolCall, Map<Long, String> referencedCampaigns) {
        Map<String, Object> input = toolCall.arguments();

        return switch (toolCall.name()) {
            case DashboardToolDefinitions.LIST_CAMPAIGNS -> {
                CampaignsOverviewResponse overview = dashboardService.getCampaignsOverview();
                for (CampaignOverviewItem item : overview.getCampaigns()) {
                    referencedCampaigns.put(item.getCampaignId(), item.getCampaignName());
                }
                yield overview;
            }
            case DashboardToolDefinitions.GET_CAMPAIGN_STATS -> {
                CampaignStatsResponse stats = dashboardService.getCampaignStats(requireCampaignId(input));
                referencedCampaigns.put(stats.getCampaignId(), stats.getCampaignName());
                yield stats;
            }
            case DashboardToolDefinitions.GET_CONVERSION_FUNNEL -> {
                ConversionFunnelResponse funnel = dashboardService.getConversionFunnel(requireCampaignId(input));
                referencedCampaigns.put(funnel.getCampaignId(), funnel.getCampaignName());
                yield funnel;
            }
            case DashboardToolDefinitions.GET_TOP_REFERRERS -> {
                Integer limit = optionalInt(input, "limit");
                TopReferrersResponse topReferrers = dashboardService.getTopReferrers(requireCampaignId(input), limit);
                referencedCampaigns.put(topReferrers.getCampaignId(), topReferrers.getCampaignName());
                yield topReferrers;
            }
            case DashboardToolDefinitions.GET_TIME_SERIES -> {
                LocalDate startDate = optionalDate(input, "startDate");
                LocalDate endDate = optionalDate(input, "endDate");
                String granularity = (String) input.get("granularity");
                TimeSeriesResponse timeSeries = dashboardService.getTimeSeries(requireCampaignId(input), startDate, endDate, granularity);
                referencedCampaigns.put(timeSeries.getCampaignId(), timeSeries.getCampaignName());
                yield timeSeries;
            }
            case DashboardToolDefinitions.GET_REWARD_SUMMARY -> {
                RewardSummaryResponse summary = dashboardService.getRewardSummary(requireCampaignId(input));
                referencedCampaigns.put(summary.getCampaignId(), summary.getCampaignName());
                yield summary;
            }
            default -> throw new BadRequestException("Unknown tool: " + toolCall.name());
        };
    }

    private Long requireCampaignId(Map<String, Object> input) {
        Object raw = input.get("campaignId");
        if (raw == null) {
            throw new BadRequestException("campaignId is required for this tool");
        }
        return ((Number) raw).longValue();
    }

    private Integer optionalInt(Map<String, Object> input, String key) {
        Object raw = input.get(key);
        return raw == null ? null : ((Number) raw).intValue();
    }

    private LocalDate optionalDate(Map<String, Object> input, String key) {
        Object raw = input.get(key);
        return raw == null ? null : LocalDate.parse(raw.toString());
    }

    private String toJson(Object result) {
        try {
            return objectMapper.writeValueAsString(result);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to serialize tool result", e);
        }
    }

    private AskDashboardResponse buildResponse(ChatResult result, List<String> toolsUsed, Map<Long, String> referencedCampaigns) {
        String answer = result.text();
        if (answer == null || answer.isBlank()) {
            answer = "I don't have a clear answer for that based on the available dashboard data.";
        }
        return new AskDashboardResponse(answer, toolsUsed, toReferencedCampaignList(referencedCampaigns));
    }

    private List<ReferencedCampaign> toReferencedCampaignList(Map<Long, String> referencedCampaigns) {
        return referencedCampaigns.entrySet().stream()
                .map(e -> new ReferencedCampaign(e.getKey(), e.getValue()))
                .toList();
    }

    private String buildSystemPrompt(Long campaignId) {
        String base = """
                You are a data analyst answering a company admin's question about their referral \
                marketing campaigns, using only the tools provided. Today's date is %s.
                When the user references a campaign by name, call list_campaigns first to resolve it to \
                a campaignId. When the user asks about a relative period ("last month", "this quarter"), \
                compute explicit startDate/endDate yourself before calling get_time_series. Answer \
                concisely, cite specific numbers from tool results, and say so plainly if the data \
                doesn't support an answer - never guess or fabricate a figure.""".formatted(LocalDate.now());
        if (campaignId == null) {
            return base;
        }
        return base + "\nThe user is currently viewing campaignId=" + campaignId
                + " - prefer this campaign if the question doesn't name a different one.";
    }
}
