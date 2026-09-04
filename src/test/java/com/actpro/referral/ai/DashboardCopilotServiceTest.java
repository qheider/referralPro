package com.actpro.referral.ai;

import com.actpro.referral.ai.dto.AskDashboardResponse;
import com.actpro.referral.ai.provider.ChatRequest;
import com.actpro.referral.ai.provider.ChatResult;
import com.actpro.referral.ai.provider.ConversationTurn;
import com.actpro.referral.ai.provider.ToolCall;
import com.actpro.referral.common.exception.BadRequestException;
import com.actpro.referral.common.exception.NotFoundException;
import com.actpro.referral.dashboard.DashboardService;
import com.actpro.referral.dashboard.dto.CampaignOverviewItem;
import com.actpro.referral.dashboard.dto.CampaignStatsResponse;
import com.actpro.referral.dashboard.dto.CampaignsOverviewResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DashboardCopilotServiceTest {

    @Mock
    private AiClient aiClient;

    @Mock
    private DashboardService dashboardService;

    private DashboardCopilotService dashboardCopilotService;

    @BeforeEach
    void setUp() {
        dashboardCopilotService = new DashboardCopilotService(aiClient, new DashboardToolDefinitions(), dashboardService, new ObjectMapper());
        setField("enabled", true);
        setField("maxToolIterations", 6);
    }

    @Test
    void shouldAnswerDirectlyWithoutToolCallWhenNoDataNeeded() {
        when(aiClient.isConfigured()).thenReturn(true);
        when(aiClient.chat(any())).thenReturn(textResult("Hi there, how can I help?"));

        AskDashboardResponse response = dashboardCopilotService.ask("hello", null);

        assertEquals("Hi there, how can I help?", response.answer());
        assertTrue(response.toolsUsed().isEmpty());
        verifyNoInteractions(dashboardService);
    }

    @Test
    void shouldDispatchSingleToolCallAndReturnFinalAnswer() {
        when(aiClient.isConfigured()).thenReturn(true);
        CampaignStatsResponse stats = new CampaignStatsResponse(
                5L, "Summer Sale", "ACTIVE", 100L, 200L, 30L,
                50.0, 15.0, 10L, BigDecimal.TEN, 5L, BigDecimal.ONE
        );
        when(dashboardService.getCampaignStats(5L)).thenReturn(stats);
        when(aiClient.chat(any()))
                .thenReturn(toolCallResult(DashboardToolDefinitions.GET_CAMPAIGN_STATS, Map.of("campaignId", 5)))
                .thenReturn(textResult("Summer Sale converted 30 referrals."));

        AskDashboardResponse response = dashboardCopilotService.ask("How is campaign 5 doing?", null);

        assertEquals("Summer Sale converted 30 referrals.", response.answer());
        assertEquals(List.of(DashboardToolDefinitions.GET_CAMPAIGN_STATS), response.toolsUsed());
        assertEquals(1, response.campaigns().size());
        assertEquals("Summer Sale", response.campaigns().get(0).campaignName());
        verify(dashboardService).getCampaignStats(5L);
    }

    @Test
    void shouldResolveCampaignByNameViaListCampaignsBeforeGettingStats() {
        when(aiClient.isConfigured()).thenReturn(true);
        CampaignsOverviewResponse overview = new CampaignsOverviewResponse(
                1L, "Acme Co", List.of(new CampaignOverviewItem(7L, "Summer Sale", "ACTIVE", 10L, 20L, 3L, 30.0))
        );
        when(dashboardService.getCampaignsOverview()).thenReturn(overview);
        CampaignStatsResponse stats = new CampaignStatsResponse(
                7L, "Summer Sale", "ACTIVE", 10L, 20L, 3L, 30.0, 15.0, 2L, BigDecimal.ONE, 1L, BigDecimal.ONE
        );
        when(dashboardService.getCampaignStats(7L)).thenReturn(stats);
        when(aiClient.chat(any()))
                .thenReturn(toolCallResult(DashboardToolDefinitions.LIST_CAMPAIGNS, Map.of()))
                .thenReturn(toolCallResult(DashboardToolDefinitions.GET_CAMPAIGN_STATS, Map.of("campaignId", 7)))
                .thenReturn(textResult("Summer Sale converted 3 referrals."));

        AskDashboardResponse response = dashboardCopilotService.ask("How is the summer campaign doing?", null);

        assertEquals(List.of(DashboardToolDefinitions.LIST_CAMPAIGNS, DashboardToolDefinitions.GET_CAMPAIGN_STATS), response.toolsUsed());
        verify(dashboardService).getCampaignsOverview();
        verify(dashboardService).getCampaignStats(7L);
    }

    @Test
    void shouldReturnErrorToolResultWhenCampaignNotFoundForTenant() {
        when(aiClient.isConfigured()).thenReturn(true);
        when(dashboardService.getCampaignStats(99L)).thenThrow(new NotFoundException("Campaign not found"));
        when(aiClient.chat(any()))
                .thenReturn(toolCallResult(DashboardToolDefinitions.GET_CAMPAIGN_STATS, Map.of("campaignId", 99)))
                .thenReturn(textResult("I couldn't find that campaign."));

        AskDashboardResponse response = dashboardCopilotService.ask("How is campaign 99 doing?", null);

        assertEquals("I couldn't find that campaign.", response.answer());

        ArgumentCaptor<ChatRequest> captor = ArgumentCaptor.forClass(ChatRequest.class);
        verify(aiClient, times(2)).chat(captor.capture());
        ChatRequest secondCall = captor.getAllValues().get(1);
        boolean sawErrorToolResult = secondCall.turns().stream()
                .filter(ConversationTurn.ToolResultTurn.class::isInstance)
                .map(ConversationTurn.ToolResultTurn.class::cast)
                .flatMap(t -> t.outcomes().stream())
                .anyMatch(outcome -> outcome.isError());
        assertTrue(sawErrorToolResult, "expected an isError tool outcome to be sent back after the NotFoundException");
    }

    @Test
    void shouldCapToolLoopAtMaxIterationsRatherThanLoopForever() {
        when(aiClient.isConfigured()).thenReturn(true);
        setField("maxToolIterations", 3);
        when(dashboardService.getCampaignsOverview()).thenReturn(
                new CampaignsOverviewResponse(1L, "Acme Co", List.of()));
        when(aiClient.chat(any()))
                .thenReturn(toolCallResult(DashboardToolDefinitions.LIST_CAMPAIGNS, Map.of()));

        AskDashboardResponse response = dashboardCopilotService.ask("Loop forever?", null);

        verify(aiClient, times(3)).chat(any());
        assertTrue(response.answer() != null && !response.answer().isBlank());
    }

    @Test
    void shouldRejectWhenAiCopilotDisabled() {
        setField("enabled", false);

        assertThrows(BadRequestException.class, () -> dashboardCopilotService.ask("hello", null));

        verifyNoInteractions(aiClient);
    }

    @Test
    void shouldRejectWhenAiClientNotConfigured() {
        when(aiClient.isConfigured()).thenReturn(false);

        assertThrows(BadRequestException.class, () -> dashboardCopilotService.ask("hello", null));

        verify(aiClient, never()).chat(any());
    }

    private void setField(String name, Object value) {
        try {
            var field = DashboardCopilotService.class.getDeclaredField(name);
            field.setAccessible(true);
            field.set(dashboardCopilotService, value);
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
    }

    private ChatResult textResult(String text) {
        return new ChatResult(text, List.of());
    }

    private ChatResult toolCallResult(String toolName, Map<String, Object> arguments) {
        return new ChatResult("", List.of(new ToolCall("tool_1", toolName, arguments)));
    }
}
