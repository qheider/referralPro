export interface AmbassadorSummary {
  id: number;
  userId: number;
  firstName: string;
  lastName: string;
  email: string;
  displayName?: string | null;
  status: 'INVITED' | 'ACTIVE' | 'INACTIVE' | 'SUSPENDED';
  assignedCampaigns: number;
  totalRegistrations: number;
  successfulRentals: number;
  conversionRate: number;
  createdAt: string;
}

export interface AmbassadorReferralLink {
  id: number;
  campaignId: number;
  campaignName: string;
  publicToken: string;
  referralUrl: string;
  qrCodeUrl: string;
  destinationUrl?: string | null;
  status: 'ACTIVE' | 'DISABLED' | 'EXPIRED';
  clickCount: number;
  expiresAt?: string | null;
}

export interface AmbassadorDetail extends AmbassadorSummary {
  phone?: string | null;
  bio?: string | null;
  socialMediaPlatform?: string | null;
  socialMediaHandle?: string | null;
  profileImageUrl?: string | null;
  ambassadorCode: string;
  joinedAt?: string | null;
  referralLinks: AmbassadorReferralLink[];
}

export interface AmbassadorPageResponse {
  content: AmbassadorSummary[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
  first: boolean;
  last: boolean;
}

export interface CreateAmbassadorRequest {
  firstName: string;
  lastName: string;
  email: string;
  phone?: string | null;
  displayName?: string | null;
  socialMediaPlatform?: string | null;
  socialMediaHandle?: string | null;
}

export interface UpdateAmbassadorRequest {
  firstName: string;
  lastName: string;
  displayName?: string | null;
  phone?: string | null;
  status: 'INVITED' | 'ACTIVE' | 'INACTIVE' | 'SUSPENDED';
  socialMediaPlatform?: string | null;
  socialMediaHandle?: string | null;
  bio?: string | null;
  profileImageUrl?: string | null;
}
