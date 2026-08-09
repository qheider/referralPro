import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable, map } from 'rxjs';
import { environment } from '../../../environments/environment';
import { ApiResponse } from '../../shared/models/api-response.model';
import { SubmitReferralLeadRequest, SubmitReferralLeadResponse } from '../../shared/models/referral-lead.model';

@Injectable({
  providedIn: 'root'
})
export class ReferralLeadService {
  constructor(private http: HttpClient) {}

  // Public - no auth required. `sessionId` is the click-attribution session id forwarded as a
  // query param from ReferralClickService's redirect (see ReferralRedirectController / /r/{token})
  // - the rp_attr_session cookie it's normally read from is SameSite=Lax and set on the backend's
  // own origin, so it isn't attached to this cross-origin POST; sending it explicitly here is the
  // fallback ReferralLeadController accepts.
  submitLead(
    token: string,
    sessionId: string | null,
    request: SubmitReferralLeadRequest
  ): Observable<SubmitReferralLeadResponse> {
    let url = `${environment.apiUrl}/referral-links/${encodeURIComponent(token)}/leads`;
    if (sessionId) {
      url += `?s=${encodeURIComponent(sessionId)}`;
    }

    return this.http
      .post<ApiResponse<SubmitReferralLeadResponse>>(url, request, { withCredentials: true })
      .pipe(map(response => this.unwrapResponse(response, 'Unable to submit registration.')));
  }

  private unwrapResponse<T>(response: ApiResponse<T>, fallbackMessage: string): T {
    if (!response.success || response.data === undefined) {
      throw new Error(response.message || fallbackMessage);
    }

    return response.data;
  }
}
