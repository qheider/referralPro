export interface ReferralLinkSummary {
  referralLinkId: number;
  publicToken: string;
  publicUrl: string;
  destinationUrl: string;
  status: string;
  clickCount: number;
  expiresAt: string | null;
}

export interface AmbassadorDashboardResponse {
  ambassadorId: number;
  displayName: string;
  activeCampaigns: number;
  totalClicks: number;
  totalRegistrations: number;
  totalBookingsStarted: number;
  totalCompletedRentals: number;
  registrationConversionRate: number;
  rentalConversionRate: number;
  recentReferrals: AmbassadorRecentReferral[];
}

export interface AmbassadorRecentReferral {
  referralId: number;
  campaignId: number;
  campaignName: string;
  customerName: string;
  customerEmail: string | null;
  status: string;
  registeredAt: string | null;
  convertedAt: string | null;
}

export interface AmbassadorCampaignOverview {
  assignmentId: number;
  campaignId: number;
  campaignName: string;
  description: string;
  status: string;
  startDate: string;
  endDate: string;
  conversionEventName: string;
  referrerRewardValue: number;
  refereeRewardValue: number;
  rewardType: string;
  clickCount: number;
  registrationCount: number;
  completedRentalCount: number;
  registrationConversionRate: number;
  referralLink: ReferralLinkSummary;
}

export interface AmbassadorCampaignDetail extends AmbassadorCampaignOverview {
  landingPageUrl: string;
  bookingStartedCount: number;
  rentalConversionRate: number;
}

export interface AmbassadorReferral {
  referralId: number;
  campaignId: number;
  campaignName: string;
  referralCode: string;
  customerName: string;
  customerEmail: string | null;
  status: string;
  createdAt: string;
  registeredAt: string | null;
  convertedAt: string | null;
  bookingId: string | null;
  rentalId: string | null;
  discountAmount: number | null;
  currency: string | null;
}

export interface AmbassadorReferralHistoryResponse {
  content: AmbassadorReferral[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
}

export interface AmbassadorCampaignPerformance {
  campaignId: number;
  campaignName: string;
  clicks: number;
  registrations: number;
  completedRentals: number;
  registrationConversionRate: number;
  rentalConversionRate: number;
}

export interface AmbassadorPerformanceTrend {
  date: string;
  clicks: number;
  registrations: number;
  completedRentals: number;
}

export interface AmbassadorAnalyticsResponse {
  fromDate: string;
  toDate: string;
  totalClicks: number;
  totalRegistrations: number;
  totalBookingsStarted: number;
  totalCompletedRentals: number;
  registrationConversionRate: number;
  rentalConversionRate: number;
  campaigns: AmbassadorCampaignPerformance[];
  trends: AmbassadorPerformanceTrend[];
}

export interface AmbassadorProfile {
  ambassadorId: number;
  userId: number;
  firstName: string | null;
  lastName: string | null;
  email: string;
  displayName: string | null;
  phone: string | null;
  bio: string | null;
  socialMediaPlatform: string | null;
  socialMediaHandle: string | null;
  profileImageUrl: string | null;
  ambassadorCode: string;
  ambassadorStatus: string;
  userStatus: string;
  joinedAt: string | null;
}

export interface UpdateAmbassadorProfileRequest {
  firstName: string | null;
  lastName: string | null;
  displayName: string | null;
  phone: string | null;
  bio: string | null;
  socialMediaPlatform: string | null;
  socialMediaHandle: string | null;
  profileImageUrl: string | null;
}
