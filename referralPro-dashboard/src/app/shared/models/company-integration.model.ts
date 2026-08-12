export type CompanyIntegrationStatus = 'NOT_CONFIGURED' | 'PENDING_VERIFICATION' | 'ACTIVE' | 'DISABLED' | 'ERROR';

export type IntegrationAuthType = 'NONE' | 'API_KEY' | 'BEARER_TOKEN' | 'BASIC';

export type ApiSubmissionStatus = 'PENDING' | 'PROCESSING' | 'SUCCEEDED' | 'RETRY_SCHEDULED' | 'PERMANENTLY_FAILED' | 'CANCELLED';

export type AttemptOutcome = 'SUCCESS' | 'FAILURE';

export type FailureCategory = 'NONE' | 'TIMEOUT' | 'CONNECTION_ERROR' | 'RATE_LIMITED' | 'SERVER_ERROR' | 'CLIENT_ERROR' | 'AUTH_ERROR';

export type WebhookEventStatus = 'RECEIVED' | 'PROCESSING' | 'PROCESSED' | 'IGNORED' | 'RETRY_SCHEDULED' | 'MANUAL_REVIEW';

// Credential fields are conditionally required per authType (validated server-side, not here).
// Omitting all credential fields for the current auth type on an update keeps the existing
// stored credentials unchanged; supplying any of them replaces the whole set for that type.
export interface UpdateCompanyIntegrationConfigRequest {
  apiBaseUrl: string;
  authType: IntegrationAuthType;
  apiKeyHeaderName?: string | null;
  apiKeyValue?: string | null;
  bearerToken?: string | null;
  basicUsername?: string | null;
  basicPassword?: string | null;
  requestTimeoutMs?: number | null;
  maxRetryAttempts?: number | null;
  statusMappingJson?: string | null;
  rewardMappingJson?: string | null;
}

// Never carries a raw or decrypted credential value - only hasCredentials.
export interface CompanyIntegrationConfigResponse {
  id: number;
  status: CompanyIntegrationStatus;
  apiBaseUrl?: string | null;
  authType: IntegrationAuthType;
  hasCredentials: boolean;
  requestTimeoutMs: number;
  maxRetryAttempts: number;
  statusMappingJson?: string | null;
  rewardMappingJson?: string | null;
  lastTestedAt?: string | null;
  lastTestResult?: string | null;
  lastTestMessage?: string | null;
  webhookPublicId: string;
  webhookUrl: string;
  hasWebhookSigningSecret: boolean;
  createdAt: string;
  updatedAt: string;
}

export interface TestConnectionResponse {
  success: boolean;
  httpStatus?: number | null;
  message?: string | null;
  testedAt: string;
  resultingStatus: CompanyIntegrationStatus;
}

export interface ApiSubmissionSummaryResponse {
  id: number;
  externalRequestId: string;
  status: ApiSubmissionStatus;
  attempts: number;
  maxAttempts: number;
  referralId: number;
  lastError?: string | null;
  availableAt: string;
  submittedAt?: string | null;
  createdAt: string;
}

export interface IntegrationAttemptResponse {
  attemptNumber: number;
  startedAt: string;
  completedAt?: string | null;
  httpStatus?: number | null;
  outcome: AttemptOutcome;
  failureCategory?: FailureCategory | null;
  sanitizedMessage?: string | null;
  nextRetryAt?: string | null;
}

export interface ApiSubmissionDetailResponse {
  id: number;
  externalRequestId: string;
  status: ApiSubmissionStatus;
  attemptCount: number;
  maxAttempts: number;
  referralId: number;
  lastError?: string | null;
  companyCustomerReference?: string | null;
  companyTransactionReference?: string | null;
  availableAt: string;
  submittedAt?: string | null;
  createdAt: string;
  attempts: IntegrationAttemptResponse[];
}

export interface GenerateWebhookSecretResponse {
  webhookSecret: string;
  generatedAt: string;
}

export interface WebhookEventSummaryResponse {
  id: number;
  eventId: string;
  eventType: string;
  status: WebhookEventStatus;
  matchedReferralId?: number | null;
  mappedStatus?: string | null;
  failureReason?: string | null;
  createdAt: string;
  processedAt?: string | null;
}

export interface WebhookEventDetailResponse {
  id: number;
  eventId: string;
  eventType: string;
  status: WebhookEventStatus;
  rawPayload: string;
  matchedReferralId?: number | null;
  mappedStatus?: string | null;
  failureReason?: string | null;
  attempts: number;
  maxAttempts: number;
  availableAt: string;
  createdAt: string;
  processedAt?: string | null;
}
