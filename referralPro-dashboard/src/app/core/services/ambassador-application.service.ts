import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable, map } from 'rxjs';
import { environment } from '../../../environments/environment';
import { ApiResponse } from '../../shared/models/api-response.model';
import {
  AmbassadorApplicationSubmissionResponse,
  AmbassadorRegistrationResponse,
  SubmitAmbassadorApplicationRequest
} from '../../shared/models/ambassador-application.model';

@Injectable({
  providedIn: 'root'
})
export class AmbassadorApplicationService {
  constructor(private http: HttpClient) {}

  // Public - no auth required. campaignCode is optional (present when applying through a
  // campaign's join link rather than a company's general application link).
  apply(
    companyId: number,
    campaignCode: string | null,
    request: SubmitAmbassadorApplicationRequest
  ): Observable<AmbassadorApplicationSubmissionResponse> {
    let url = `${environment.apiUrl}/ambassador-applications/apply?companyId=${companyId}`;
    if (campaignCode) {
      url += `&campaignCode=${encodeURIComponent(campaignCode)}`;
    }

    return this.http
      .post<ApiResponse<AmbassadorApplicationSubmissionResponse>>(url, request)
      .pipe(map(response => this.unwrapResponse(response, 'Unable to submit application.')));
  }

  // Public - no auth required. Instant self-service registration (see
  // AmbassadorRegistrationController): unlike apply() above, the account is provisioned
  // immediately rather than parked for admin review - only email verification gates it.
  register(
    companyId: number,
    campaignCode: string | null,
    request: SubmitAmbassadorApplicationRequest
  ): Observable<AmbassadorRegistrationResponse> {
    let url = `${environment.apiUrl}/ambassador-registrations?companyId=${companyId}`;
    if (campaignCode) {
      url += `&campaignCode=${encodeURIComponent(campaignCode)}`;
    }

    return this.http
      .post<ApiResponse<AmbassadorRegistrationResponse>>(url, request)
      .pipe(map(response => this.unwrapResponse(response, 'Unable to submit registration.')));
  }

  private unwrapResponse<T>(response: ApiResponse<T>, fallbackMessage: string): T {
    if (!response.success || response.data === undefined) {
      throw new Error(response.message || fallbackMessage);
    }

    return response.data;
  }
}
