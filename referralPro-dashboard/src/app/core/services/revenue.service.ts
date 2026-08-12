import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable, map } from 'rxjs';
import { environment } from '../../../environments/environment';
import { ApiResponse } from '../../shared/models/api-response.model';
import {
  AmbassadorRewardResponse,
  AmbassadorRewardStatus,
  CampaignRevenueReportResponse
} from '../../shared/models/revenue.model';

const BASE_URL = `${environment.apiUrl}/admin/revenue`;

@Injectable({
  providedIn: 'root'
})
export class RevenueService {
  constructor(private http: HttpClient) {}

  listRewards(params: { campaignId?: number; status?: AmbassadorRewardStatus | null; limit?: number }): Observable<AmbassadorRewardResponse[]> {
    let httpParams = new HttpParams().set('limit', String(params.limit ?? 50));
    if (params.campaignId !== undefined) {
      httpParams = httpParams.set('campaignId', String(params.campaignId));
    }
    if (params.status) {
      httpParams = httpParams.set('status', params.status);
    }

    return this.http
      .get<ApiResponse<AmbassadorRewardResponse[]>>(`${BASE_URL}/rewards`, { params: httpParams })
      .pipe(map(response => this.unwrapResponse(response, 'Unable to load rewards.')));
  }

  getReward(rewardId: number): Observable<AmbassadorRewardResponse> {
    return this.http
      .get<ApiResponse<AmbassadorRewardResponse>>(`${BASE_URL}/rewards/${rewardId}`)
      .pipe(map(response => this.unwrapResponse(response, 'Unable to load reward.')));
  }

  approveReward(rewardId: number): Observable<AmbassadorRewardResponse> {
    return this.http
      .post<ApiResponse<AmbassadorRewardResponse>>(`${BASE_URL}/rewards/${rewardId}/approve`, {})
      .pipe(map(response => this.unwrapResponse(response, 'Unable to approve reward.')));
  }

  markRewardPaid(rewardId: number): Observable<AmbassadorRewardResponse> {
    return this.http
      .post<ApiResponse<AmbassadorRewardResponse>>(`${BASE_URL}/rewards/${rewardId}/mark-paid`, {})
      .pipe(map(response => this.unwrapResponse(response, 'Unable to mark reward as paid.')));
  }

  rejectReward(rewardId: number, reason: string): Observable<AmbassadorRewardResponse> {
    return this.http
      .post<ApiResponse<AmbassadorRewardResponse>>(`${BASE_URL}/rewards/${rewardId}/reject`, { reason })
      .pipe(map(response => this.unwrapResponse(response, 'Unable to reject reward.')));
  }

  getCampaignReport(campaignId: number): Observable<CampaignRevenueReportResponse> {
    return this.http
      .get<ApiResponse<CampaignRevenueReportResponse>>(`${BASE_URL}/campaigns/${campaignId}/report`)
      .pipe(map(response => this.unwrapResponse(response, 'Unable to load campaign revenue report.')));
  }

  private unwrapResponse<T>(response: ApiResponse<T>, fallbackMessage: string): T {
    if (!response.success || response.data === undefined) {
      throw new Error(response.message || fallbackMessage);
    }

    return response.data;
  }
}
