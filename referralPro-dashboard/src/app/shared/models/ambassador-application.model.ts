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

// Instant self-service registration via a campaign's join link - the account exists immediately
// but stays unusable until the applicant clicks the onboarding email's accept-invitation link.
export type AmbassadorStatus = 'INVITED' | 'ACTIVE' | 'INACTIVE' | 'SUSPENDED';

export interface AmbassadorRegistrationResponse {
  ambassadorProfileId: number;
  status: AmbassadorStatus;
  submittedAt: string;
}
