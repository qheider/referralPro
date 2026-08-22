export interface SubmitReferralLeadRequest {
  name: string;
  email: string;
}

export type ReferralStatus = 'REGISTERED' | string;

export interface SubmitReferralLeadResponse {
  referralCode: string;
  status: ReferralStatus;
  registeredAt: string;
  /** The ambassador link's destinationUrl (with ?ref= appended), once registered - null/absent when the link has no destinationUrl. */
  redirectUrl?: string | null;
}
