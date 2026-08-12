import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable, map } from 'rxjs';
import { environment } from '../../../environments/environment';
import { ApiResponse } from '../../shared/models/api-response.model';
import {
  ApiSubmissionDetailResponse,
  ApiSubmissionStatus,
  ApiSubmissionSummaryResponse,
  CompanyIntegrationConfigResponse,
  GenerateWebhookSecretResponse,
  TestConnectionResponse,
  UpdateCompanyIntegrationConfigRequest,
  WebhookEventDetailResponse,
  WebhookEventStatus,
  WebhookEventSummaryResponse
} from '../../shared/models/company-integration.model';

const BASE_URL = `${environment.apiUrl}/admin/company-integration`;

@Injectable({
  providedIn: 'root'
})
export class CompanyIntegrationService {
  constructor(private http: HttpClient) {}

  getConfig(): Observable<CompanyIntegrationConfigResponse> {
    return this.http
      .get<ApiResponse<CompanyIntegrationConfigResponse>>(BASE_URL)
      .pipe(map(response => this.unwrapResponse(response, 'Unable to load integration configuration.')));
  }

  updateConfig(request: UpdateCompanyIntegrationConfigRequest): Observable<CompanyIntegrationConfigResponse> {
    return this.http
      .put<ApiResponse<CompanyIntegrationConfigResponse>>(BASE_URL, request)
      .pipe(map(response => this.unwrapResponse(response, 'Unable to save integration configuration.')));
  }

  testConnection(): Observable<TestConnectionResponse> {
    return this.http
      .post<ApiResponse<TestConnectionResponse>>(`${BASE_URL}/test-connection`, {})
      .pipe(map(response => this.unwrapResponse(response, 'Unable to test the connection.')));
  }

  enable(): Observable<CompanyIntegrationConfigResponse> {
    return this.http
      .post<ApiResponse<CompanyIntegrationConfigResponse>>(`${BASE_URL}/enable`, {})
      .pipe(map(response => this.unwrapResponse(response, 'Unable to enable the integration.')));
  }

  disable(): Observable<CompanyIntegrationConfigResponse> {
    return this.http
      .post<ApiResponse<CompanyIntegrationConfigResponse>>(`${BASE_URL}/disable`, {})
      .pipe(map(response => this.unwrapResponse(response, 'Unable to disable the integration.')));
  }

  listSubmissions(status?: ApiSubmissionStatus | null): Observable<ApiSubmissionSummaryResponse[]> {
    const url = status ? `${BASE_URL}/submissions?status=${status}` : `${BASE_URL}/submissions`;
    return this.http
      .get<ApiResponse<ApiSubmissionSummaryResponse[]>>(url)
      .pipe(map(response => this.unwrapResponse(response, 'Unable to load submissions.')));
  }

  getSubmission(submissionId: number): Observable<ApiSubmissionDetailResponse> {
    return this.http
      .get<ApiResponse<ApiSubmissionDetailResponse>>(`${BASE_URL}/submissions/${submissionId}`)
      .pipe(map(response => this.unwrapResponse(response, 'Unable to load submission detail.')));
  }

  generateWebhookSecret(): Observable<GenerateWebhookSecretResponse> {
    return this.http
      .post<ApiResponse<GenerateWebhookSecretResponse>>(`${BASE_URL}/webhook-secret`, {})
      .pipe(map(response => this.unwrapResponse(response, 'Unable to generate the webhook signing secret.')));
  }

  listWebhookEvents(status?: WebhookEventStatus | null): Observable<WebhookEventSummaryResponse[]> {
    const url = status ? `${BASE_URL}/webhook-events?status=${status}` : `${BASE_URL}/webhook-events`;
    return this.http
      .get<ApiResponse<WebhookEventSummaryResponse[]>>(url)
      .pipe(map(response => this.unwrapResponse(response, 'Unable to load webhook events.')));
  }

  getWebhookEvent(webhookEventId: number): Observable<WebhookEventDetailResponse> {
    return this.http
      .get<ApiResponse<WebhookEventDetailResponse>>(`${BASE_URL}/webhook-events/${webhookEventId}`)
      .pipe(map(response => this.unwrapResponse(response, 'Unable to load webhook event detail.')));
  }

  private unwrapResponse<T>(response: ApiResponse<T>, fallbackMessage: string): T {
    if (!response.success || response.data === undefined) {
      throw new Error(response.message || fallbackMessage);
    }

    return response.data;
  }
}
