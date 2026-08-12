export type AmbassadorRewardStatus = 'PENDING' | 'ELIGIBLE' | 'APPROVED' | 'PAID' | 'REJECTED' | 'REVERSED';

export interface AmbassadorRewardResponse {
  id: number;
  campaignId: number;
  campaignName: string;
  referralId: number;
  referralCode: string;
  ambassadorUserId: number;
  ambassadorName: string;
  rewardType: string;
  rewardValue: number;
  currency?: string | null;
  status: AmbassadorRewardStatus;
  holdReason?: string | null;
  rejectionReason?: string | null;
  revenueEventId: number;
  qualifyingStatus: string;
  revenueAmount?: number | null;
  currencyMismatch: boolean;
  createdAt: string;
  approvedAt?: string | null;
  paidAt?: string | null;
  rejectedAt?: string | null;
  reversedAt?: string | null;
}

export interface AmbassadorRevenueSummaryResponse {
  ambassadorUserId: number;
  ambassadorName: string;
  qualifyingEventCount: number;
  reversedEventCount: number;
  totalPendingOrEligibleValue: number;
  totalApprovedValue: number;
  totalPaidValue: number;
}

export interface CampaignRevenueReportResponse {
  campaignId: number;
  campaignName: string;
  qualifyingEventCount: number;
  reversedEventCount: number;
  mismatchedCurrencyEventCount: number;
  revenueByCurrency: Record<string, number>;
  rewardCount: number;
  totalPendingValue: number;
  totalEligibleValue: number;
  totalApprovedValue: number;
  totalPaidValue: number;
  totalRejectedValue: number;
  totalReversedValue: number;
  ambassadorLeaderboard: AmbassadorRevenueSummaryResponse[];
}
