export interface SubmitAmbassadorApplicationRequest {
  firstName: string;
  lastName: string;
  email: string;
  phone?: string | null;
  displayName?: string | null;
  bio?: string | null;
  socialMediaPlatform?: string | null;
  socialMediaHandle?: string | null;
}

export type ApplicationStatus = 'PENDING' | 'APPROVED' | 'REJECTED';

export interface AmbassadorApplicationSubmissionResponse {
  applicationId: number;
  status: ApplicationStatus;
  submittedAt: string;
}

export interface AmbassadorApplicationSummary {
  id: number;
  firstName: string;
  lastName: string;
  email: string;
  displayName?: string | null;
  campaignId?: number | null;
  campaignName?: string | null;
  status: ApplicationStatus;
  submittedAt: string;
  reviewedAt?: string | null;
}

export interface AmbassadorApplicationDetail {
  id: number;
  firstName: string;
  lastName: string;
  email: string;
  phone?: string | null;
  displayName?: string | null;
  bio?: string | null;
  socialMediaPlatform?: string | null;
  socialMediaHandle?: string | null;
  campaignId?: number | null;
  campaignName?: string | null;
  status: ApplicationStatus;
  rejectionReason?: string | null;
  reviewedByUserId?: number | null;
  reviewedAt?: string | null;
  resultingAmbassadorProfileId?: number | null;
  submittedAt: string;
}

export interface AmbassadorApplicationPageResponse {
  content: AmbassadorApplicationSummary[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
  first: boolean;
  last: boolean;
}

export interface RejectApplicationRequest {
  reason: string;
}
