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
