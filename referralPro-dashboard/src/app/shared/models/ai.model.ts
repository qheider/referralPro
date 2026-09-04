export interface AskDashboardRequest {
  question: string;
  campaignId?: number;
}

export interface ReferencedCampaign {
  campaignId: number;
  campaignName: string;
}

export interface AskDashboardResponse {
  answer: string;
  toolsUsed: string[];
  campaigns: ReferencedCampaign[];
}
