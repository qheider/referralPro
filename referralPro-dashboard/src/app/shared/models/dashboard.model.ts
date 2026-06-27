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
