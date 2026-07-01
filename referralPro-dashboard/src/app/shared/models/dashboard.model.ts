export interface CampaignOverviewItem {
  campaignId: number;
  campaignName: string;
  status: string;
  referralCount: number;
  clickCount: number;
  conversionCount: number;
  conversionRate: number;
}

export interface CampaignsOverviewResponse {
  companyId: number;
  companyName: string;
  campaigns: CampaignOverviewItem[];
}

export interface CampaignStatsResponse {
  campaignId: number;
  campaignName: string;
  status: string;
  totalReferrals: number;
  totalClicks: number;
  totalConversions: number;
  clickThroughRate: number;
  conversionRate: number;
  totalRewardsIssued: number;
  totalRewardValue: number;
  rewardsRedeemed: number;
  averageRewardValue: number;
}

export interface ConversionFunnelResponse {
  campaignId: number;
  campaignName: string;
  referralsCount: number;
  clicksCount: number;
  conversionsCount: number;
  clickThroughRate: number;
  conversionRate: number;
  overallConversionRate: number;
}

export interface TopReferrerItem {
  externalUserId: string;
  userName: string;
  userEmail: string;
  referralCount: number;
  clickCount: number;
  conversionCount: number;
  totalRewardsEarned: number;
}

export interface TopReferrersResponse {
  campaignId: number;
  campaignName: string;
  topReferrers: TopReferrerItem[];
}

export interface TimeSeriesDataPoint {
  date: string;
  referrals: number;
  clicks: number;
  conversions: number;
  rewards: number;
}

export interface TimeSeriesResponse {
  campaignId: number;
  campaignName: string;
  startDate: string;
  endDate: string;
  granularity: string;
  dataPoints: TimeSeriesDataPoint[];
}

export interface RewardTypeBreakdown {
  rewardType: string;
  count: number;
  totalValue: number;
}

export interface RewardSummaryResponse {
  campaignId: number;
  campaignName: string;
  totalIssued: number;
  totalRedeemed: number;
  redemptionRate: number;
  totalValueIssued: number;
  totalValueRedeemed: number;
  breakdownByType: RewardTypeBreakdown[];
}
