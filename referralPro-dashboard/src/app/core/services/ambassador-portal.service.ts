import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable, map } from 'rxjs';
import { environment } from '../../../environments/environment';
import { ApiResponse } from '../../shared/models/api-response.model';
import {
  AmbassadorAnalyticsResponse,
  AmbassadorCampaignDetail,
  AmbassadorCampaignOverview,
  AmbassadorDashboardResponse,
  AmbassadorProfile,
  AmbassadorReferralHistoryResponse,
  ReferralLinkSummary,
  UpdateAmbassadorProfileRequest
} from '../../shared/models/ambassador-portal.model';

@Injectable({
  providedIn: 'root'
})
export class AmbassadorPortalService {
  constructor(private http: HttpClient) {}

  getDashboard(): Observable<AmbassadorDashboardResponse> {
    return this.http
      .get<ApiResponse<AmbassadorDashboardResponse>>(`${environment.apiUrl}/ambassador/dashboard`)
      .pipe(map(response => this.unwrapResponse(response, 'Unable to load dashboard.')));
  }

  listCampaigns(): Observable<AmbassadorCampaignOverview[]> {
    return this.http
      .get<ApiResponse<AmbassadorCampaignOverview[]>>(`${environment.apiUrl}/ambassador/campaigns`)
      .pipe(map(response => this.unwrapResponse(response, 'Unable to load campaigns.')));
  }

  getCampaign(campaignId: number): Observable<AmbassadorCampaignDetail> {
    return this.http
      .get<ApiResponse<AmbassadorCampaignDetail>>(`${environment.apiUrl}/ambassador/campaigns/${campaignId}`)
      .pipe(map(response => this.unwrapResponse(response, 'Unable to load campaign.')));
  }

  listReferralLinks(): Observable<ReferralLinkSummary[]> {
    return this.http
      .get<ApiResponse<ReferralLinkSummary[]>>(`${environment.apiUrl}/ambassador/referral-links`)
      .pipe(map(response => this.unwrapResponse(response, 'Unable to load referral links.')));
  }

  listReferrals(params: {
    campaignId?: number;
    status?: string;
    fromDate?: string;
    toDate?: string;
    page?: number;
    size?: number;
  }): Observable<AmbassadorReferralHistoryResponse> {
    let httpParams = new HttpParams()
      .set('page', String(params.page ?? 0))
      .set('size', String(params.size ?? 20));

    if (params.campaignId !== undefined) {
      httpParams = httpParams.set('campaignId', String(params.campaignId));
    }
    if (params.status) {
      httpParams = httpParams.set('status', params.status);
    }
    if (params.fromDate) {
      httpParams = httpParams.set('fromDate', params.fromDate);
    }
    if (params.toDate) {
      httpParams = httpParams.set('toDate', params.toDate);
    }

    return this.http
      .get<ApiResponse<AmbassadorReferralHistoryResponse>>(`${environment.apiUrl}/ambassador/referrals`, { params: httpParams })
      .pipe(map(response => this.unwrapResponse(response, 'Unable to load referrals.')));
  }

  getAnalytics(params: { campaignId?: number; fromDate?: string; toDate?: string }): Observable<AmbassadorAnalyticsResponse> {
    let httpParams = new HttpParams();

    if (params.campaignId !== undefined) {
      httpParams = httpParams.set('campaignId', String(params.campaignId));
    }
    if (params.fromDate) {
      httpParams = httpParams.set('fromDate', params.fromDate);
    }
    if (params.toDate) {
      httpParams = httpParams.set('toDate', params.toDate);
    }

    return this.http
      .get<ApiResponse<AmbassadorAnalyticsResponse>>(`${environment.apiUrl}/ambassador/analytics`, { params: httpParams })
      .pipe(map(response => this.unwrapResponse(response, 'Unable to load analytics.')));
  }

  getProfile(): Observable<AmbassadorProfile> {
    return this.http
      .get<ApiResponse<AmbassadorProfile>>(`${environment.apiUrl}/ambassador/profile`)
      .pipe(map(response => this.unwrapResponse(response, 'Unable to load profile.')));
  }

  updateProfile(request: UpdateAmbassadorProfileRequest): Observable<AmbassadorProfile> {
    return this.http
      .put<ApiResponse<AmbassadorProfile>>(`${environment.apiUrl}/ambassador/profile`, request)
      .pipe(map(response => this.unwrapResponse(response, 'Unable to update profile.')));
  }

  private unwrapResponse<T>(response: ApiResponse<T>, fallbackMessage: string): T {
    if (!response.success || response.data === undefined) {
      throw new Error(response.message || fallbackMessage);
    }

    return response.data;
  }
}
