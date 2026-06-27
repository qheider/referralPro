import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable, catchError, map, throwError } from 'rxjs';
import { environment } from '../../../environments/environment';
import { ApiResponse } from '../../shared/models/api-response.model';
import { CampaignsOverviewResponse } from '../../shared/models/dashboard.model';

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

  private unwrapResponse<T>(response: ApiResponse<T>, fallbackMessage: string): T {
    if (!response.success || response.data === undefined) {
      throw new Error(response.message || fallbackMessage);
    }

    return response.data;
  }
}
