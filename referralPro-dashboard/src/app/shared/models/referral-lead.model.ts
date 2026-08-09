export interface SubmitReferralLeadRequest {
  name: string;
  email: string;
}

export type ReferralStatus = 'REGISTERED' | string;

export interface SubmitReferralLeadResponse {
  referralCode: string;
  status: ReferralStatus;
  registeredAt: string;
}
