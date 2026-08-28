export type RewardType = 'DISCOUNT_AMOUNT' | 'DISCOUNT_PERCENTAGE' | 'CREDIT' | 'POINTS';

export type CampaignStatus = 'DRAFT' | 'SCHEDULED' | 'ACTIVE' | 'PAUSED' | 'EXPIRED' | 'CLOSED' | 'ARCHIVED';

export interface CreateCampaignRequest {
  name: string;
  description?: string | null;
  qualifyingConditions?: string | null;
  incentiveDescription?: string | null;
  termsUrl?: string | null;
  budgetCap?: number | null;
  landingPageUrl: string;
  // When true (and landingPageUrl is set), ambassador links/QR for this campaign point straight at
  // landingPageUrl instead of ReferralPro's own redirect + lead-capture page. Defaults to false.
  directToLandingPageEnabled: boolean;
  startDate: string;
  endDate: string;
  ambassadorEnrollmentStart: string;
  ambassadorEnrollmentEnd: string;
  rewardType: RewardType;
  referrerRewardValue: number;
  refereeRewardValue: number;
  conversionEventName: string;
}

// Partial update - only send fields the admin actually changed. Reward terms and startDate are
// locked by the backend once the campaign leaves DRAFT.
export interface UpdateCampaignRequest {
  name?: string | null;
  description?: string | null;
  qualifyingConditions?: string | null;
  incentiveDescription?: string | null;
  termsUrl?: string | null;
  budgetCap?: number | null;
  landingPageUrl?: string | null;
  directToLandingPageEnabled?: boolean | null;
  startDate?: string | null;
  endDate?: string | null;
  ambassadorEnrollmentStart?: string | null;
  ambassadorEnrollmentEnd?: string | null;
  rewardType?: RewardType | null;
  referrerRewardValue?: number | null;
  refereeRewardValue?: number | null;
  conversionEventName?: string | null;
}

export interface CampaignResponse {
  campaignId: number;
  campaignCode: string;
  joinLink: string;
  name: string;
  description?: string | null;
  qualifyingConditions?: string | null;
  incentiveDescription?: string | null;
  termsUrl?: string | null;
  budgetCap?: number | null;
  landingPageUrl: string;
  directToLandingPageEnabled: boolean;
  startDate: string;
  endDate: string;
  ambassadorEnrollmentStart: string;
  ambassadorEnrollmentEnd: string;
  rewardType: RewardType;
  referrerRewardValue: number;
  refereeRewardValue: number;
  conversionEventName: string;
  status: CampaignStatus;
  createdAt: string;
}

export interface PublicCampaignResponse {
  campaignCode: string;
  companyId: number;
  companyName: string;
  campaignName: string;
  description?: string | null;
  enrollmentOpen: boolean;
  unavailableReason?: string | null;
}
