import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable, catchError, map, throwError } from 'rxjs';
import { environment } from '../../../environments/environment';
import { ApiResponse } from '../../shared/models/api-response.model';
import {
  CampaignStatsResponse,
  CampaignsOverviewResponse,
  ConversionFunnelResponse,
  RewardSummaryResponse,
  TimeSeriesResponse,
  TopReferrersResponse
} from '../../shared/models/dashboard.model';

@Injectable({
  providedIn: 'root'
})
export class DashboardService {
  constructor(private http: HttpClient) {}

  getCampaignsOverview(): Observable<CampaignsOverviewResponse> {
    return this.http
      .get<ApiResponse<CampaignsOverviewResponse>>(`${environment.apiUrl}/dashboard/campaigns/overview`)
      .pipe(
        map(response => this.unwrapResponse(response, 'Campaign overview request did not return data')),
        catchError(error => {
          console.error('Failed to load campaign overview:', error);
          return throwError(() => error);
        })
      );
  }

  getCampaignStats(campaignId: number): Observable<CampaignStatsResponse> {
    return this.http
      .get<ApiResponse<CampaignStatsResponse>>(`${environment.apiUrl}/dashboard/campaigns/${campaignId}/stats`)
      .pipe(
        map(response => this.unwrapResponse(response, 'Campaign stats request did not return data')),
        catchError(error => {
          console.error('Failed to load campaign stats:', error);
          return throwError(() => error);
        })
      );
  }

  getConversionFunnel(campaignId: number): Observable<ConversionFunnelResponse> {
    return this.http
      .get<ApiResponse<ConversionFunnelResponse>>(`${environment.apiUrl}/dashboard/campaigns/${campaignId}/funnel`)
      .pipe(
        map(response => this.unwrapResponse(response, 'Conversion funnel request did not return data')),
        catchError(error => {
          console.error('Failed to load conversion funnel:', error);
          return throwError(() => error);
        })
      );
  }

  getTopReferrers(campaignId: number, limit = 10): Observable<TopReferrersResponse> {
    return this.http
      .get<ApiResponse<TopReferrersResponse>>(
        `${environment.apiUrl}/dashboard/campaigns/${campaignId}/top-referrers?limit=${limit}`
      )
      .pipe(
        map(response => this.unwrapResponse(response, 'Top referrers request did not return data')),
        catchError(error => {
          console.error('Failed to load top referrers:', error);
          return throwError(() => error);
        })
      );
  }

  getTimeSeries(campaignId: number): Observable<TimeSeriesResponse> {
    return this.http
      .get<ApiResponse<TimeSeriesResponse>>(`${environment.apiUrl}/dashboard/campaigns/${campaignId}/metrics/time-series`)
      .pipe(
        map(response => this.unwrapResponse(response, 'Time series request did not return data')),
        catchError(error => {
          console.error('Failed to load time series:', error);
          return throwError(() => error);
        })
      );
  }

  getRewardSummary(campaignId: number): Observable<RewardSummaryResponse> {
    return this.http
      .get<ApiResponse<RewardSummaryResponse>>(`${environment.apiUrl}/dashboard/campaigns/${campaignId}/rewards/summary`)
      .pipe(
        map(response => this.unwrapResponse(response, 'Reward summary request did not return data')),
        catchError(error => {
          console.error('Failed to load reward summary:', error);
          return throwError(() => error);
        })
      );
  }

  private unwrapResponse<T>(response: ApiResponse<T>, fallbackMessage: string): T {
    if (!response.success || response.data === undefined) {
      throw new Error(response.message || fallbackMessage);
    }

    return response.data;
  }
}
