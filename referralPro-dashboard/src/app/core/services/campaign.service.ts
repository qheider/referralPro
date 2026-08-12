import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable, map } from 'rxjs';
import { environment } from '../../../environments/environment';
import { ApiResponse } from '../../shared/models/api-response.model';
import {
  CampaignResponse,
  CreateCampaignRequest,
  PublicCampaignResponse,
  UpdateCampaignRequest
} from '../../shared/models/campaign.model';

@Injectable({
  providedIn: 'root'
})
export class CampaignService {
  constructor(private http: HttpClient) {}

  createCampaign(companyId: number, request: CreateCampaignRequest): Observable<CampaignResponse> {
    return this.http
      .post<ApiResponse<CampaignResponse>>(`${environment.apiUrl}/companies/${companyId}/campaigns`, request)
      .pipe(map(response => this.unwrapResponse(response, 'Unable to create campaign.')));
  }

  listCampaigns(companyId: number): Observable<CampaignResponse[]> {
    return this.http
      .get<ApiResponse<CampaignResponse[]>>(`${environment.apiUrl}/companies/${companyId}/campaigns`)
      .pipe(map(response => this.unwrapResponse(response, 'Unable to load campaigns.')));
  }

  getCampaign(companyId: number, campaignId: number): Observable<CampaignResponse> {
    return this.http
      .get<ApiResponse<CampaignResponse>>(`${environment.apiUrl}/companies/${companyId}/campaigns/${campaignId}`)
      .pipe(map(response => this.unwrapResponse(response, 'Unable to load campaign.')));
  }

  updateCampaign(companyId: number, campaignId: number, request: UpdateCampaignRequest): Observable<CampaignResponse> {
    return this.http
      .put<ApiResponse<CampaignResponse>>(`${environment.apiUrl}/companies/${companyId}/campaigns/${campaignId}`, request)
      .pipe(map(response => this.unwrapResponse(response, 'Unable to update campaign.')));
  }

  publishCampaign(companyId: number, campaignId: number): Observable<CampaignResponse> {
    return this.transition(companyId, campaignId, 'publish');
  }

  pauseCampaign(companyId: number, campaignId: number): Observable<CampaignResponse> {
    return this.transition(companyId, campaignId, 'pause');
  }

  resumeCampaign(companyId: number, campaignId: number): Observable<CampaignResponse> {
    return this.transition(companyId, campaignId, 'resume');
  }

  closeCampaign(companyId: number, campaignId: number): Observable<CampaignResponse> {
    return this.transition(companyId, campaignId, 'close');
  }

  archiveCampaign(companyId: number, campaignId: number): Observable<CampaignResponse> {
    return this.transition(companyId, campaignId, 'archive');
  }

  // Public - no auth required. Used by the /join/:campaignCode landing page.
  resolveJoinLink(campaignCode: string): Observable<PublicCampaignResponse> {
    return this.http
      .get<ApiResponse<PublicCampaignResponse>>(`${environment.apiUrl}/campaigns/join/${campaignCode}`)
      .pipe(map(response => this.unwrapResponse(response, 'Unable to load this campaign.')));
  }

  private transition(companyId: number, campaignId: number, action: string): Observable<CampaignResponse> {
    return this.http
      .post<ApiResponse<CampaignResponse>>(
        `${environment.apiUrl}/companies/${companyId}/campaigns/${campaignId}/${action}`,
        {}
      )
      .pipe(map(response => this.unwrapResponse(response, `Unable to ${action} campaign.`)));
  }

  private unwrapResponse<T>(response: ApiResponse<T>, fallbackMessage: string): T {
    if (!response.success || response.data === undefined) {
      throw new Error(response.message || fallbackMessage);
    }

    return response.data;
  }
}
