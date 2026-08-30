package com.actpro.referral.ai;

import com.actpro.referral.ai.provider.ToolSpec;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Builds the tool/function schemas exposed to the dashboard copilot - one per
 * {@code DashboardService} analytics method. Each is a plain JSON Schema object, so the same
 * {@link ToolSpec} feeds whichever {@link com.actpro.referral.ai.provider.AiProviderClient} is
 * configured (Anthropic and OpenAI both accept JSON Schema tool parameters natively). Deliberately
 * none of these take a companyId parameter: every dispatched call goes straight to the unmodified
 * DashboardService method, which derives the tenant from CurrentUserService itself, so the model
 * can never even ask for another company's data.
 */
@Component
public class DashboardToolDefinitions {

    public static final String LIST_CAMPAIGNS = "list_campaigns";
    public static final String GET_CAMPAIGN_STATS = "get_campaign_stats";
    public static final String GET_CONVERSION_FUNNEL = "get_conversion_funnel";
    public static final String GET_TOP_REFERRERS = "get_top_referrers";
    public static final String GET_TIME_SERIES = "get_time_series";
    public static final String GET_REWARD_SUMMARY = "get_reward_summary";

    private static final String CAMPAIGN_ID_DESCRIPTION =
            "The numeric id of the campaign, resolved via list_campaigns if the user named it rather than gave an id.";

    public List<ToolSpec> all() {
        return List.of(
                listCampaigns(),
                getCampaignStats(),
                getConversionFunnel(),
                getTopReferrers(),
                getTimeSeries(),
                getRewardSummary()
        );
    }

    private ToolSpec listCampaigns() {
        return new ToolSpec(LIST_CAMPAIGNS, """
                List all campaigns for the current company, each with its id, name, status, and \
                top-line referral/click/conversion counts and conversion rate. Call this FIRST \
                whenever the question references a campaign by name (e.g. "the summer campaign") \
                rather than a numeric id, so you can resolve the name to a campaignId before calling \
                any other tool. Also use it directly for questions comparing all campaigns at once \
                (e.g. "which campaign converted best last month").""",
                objectSchema(Map.of(), List.of()));
    }

    private ToolSpec getCampaignStats() {
        return new ToolSpec(GET_CAMPAIGN_STATS, """
                Get aggregate lifetime totals for one campaign - referrals, clicks, conversions, \
                rewards issued/redeemed, reward value, click-through rate, conversion rate. Use for \
                single-campaign overall-performance questions.""",
                campaignIdOnlySchema());
    }

    private ToolSpec getConversionFunnel() {
        return new ToolSpec(GET_CONVERSION_FUNNEL, """
                Get the referral -> click -> conversion funnel counts and stage-to-stage rates for \
                one campaign. Use for drop-off / funnel-shape questions.""",
                campaignIdOnlySchema());
    }

    private ToolSpec getTopReferrers() {
        Map<String, Object> properties = new LinkedHashMap<>();
        properties.put("campaignId", Map.of("type", "integer", "description", CAMPAIGN_ID_DESCRIPTION));
        properties.put("limit", Map.of("type", "integer", "description", "Max referrers to return, default 10."));
        return new ToolSpec(GET_TOP_REFERRERS, """
                Get the top referring users for one campaign, ranked by conversions then referrals. \
                Use for "who is my best referrer" / "top performers" questions.""",
                objectSchema(properties, List.of("campaignId")));
    }

    private ToolSpec getTimeSeries() {
        Map<String, Object> properties = new LinkedHashMap<>();
        properties.put("campaignId", Map.of("type", "integer", "description", CAMPAIGN_ID_DESCRIPTION));
        properties.put("startDate", Map.of(
                "type", "string",
                "description", "ISO-8601 date, e.g. 2026-07-01. Omit to default to 30 days before endDate."));
        properties.put("endDate", Map.of(
                "type", "string",
                "description", "ISO-8601 date, e.g. 2026-07-31. Omit to default to today."));
        properties.put("granularity", Map.of(
                "type", "string",
                "enum", List.of("daily"),
                "description", "Time bucket size. Only \"daily\" is supported today."));
        return new ToolSpec(GET_TIME_SERIES, """
                Get daily referral/click/conversion/reward counts for one campaign over a date range. \
                Use for trend questions. Defaults to the last 30 days if no dates given - compute \
                explicit startDate/endDate yourself for phrases like "last month".""",
                objectSchema(properties, List.of("campaignId")));
    }

    private ToolSpec getRewardSummary() {
        return new ToolSpec(GET_REWARD_SUMMARY, """
                Get reward issuance/redemption totals and a breakdown by reward type for one campaign. \
                Use for reward-payout or redemption-rate questions.""",
                campaignIdOnlySchema());
    }

    private Map<String, Object> campaignIdOnlySchema() {
        Map<String, Object> properties = Map.of(
                "campaignId", Map.of("type", "integer", "description", CAMPAIGN_ID_DESCRIPTION));
        return objectSchema(properties, List.of("campaignId"));
    }

    private Map<String, Object> objectSchema(Map<String, Object> properties, List<String> required) {
        Map<String, Object> schema = new LinkedHashMap<>();
        schema.put("type", "object");
        schema.put("properties", properties);
        schema.put("required", required);
        return schema;
    }
}
