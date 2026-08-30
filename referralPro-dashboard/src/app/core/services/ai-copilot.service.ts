import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable, catchError, map, throwError } from 'rxjs';
import { environment } from '../../../environments/environment';
import { ApiResponse } from '../../shared/models/api-response.model';
import { AskDashboardRequest, AskDashboardResponse } from '../../shared/models/ai.model';

@Injectable({
  providedIn: 'root'
})
export class AiCopilotService {
  constructor(private http: HttpClient) {}

  askDashboard(request: AskDashboardRequest): Observable<AskDashboardResponse> {
    return this.http
      .post<ApiResponse<AskDashboardResponse>>(`${environment.apiUrl}/ai/dashboard/ask`, request)
      .pipe(
        map(response => this.unwrapResponse(response, 'AI copilot request did not return data')),
        catchError(error => {
          console.error('AI copilot request failed:', error);
          return throwError(() => error);
        })
      );
  }

  private unwrapResponse<T>(response: ApiResponse<T>, fallbackMessage: string): T {
    if (!response || !response.success) {
      throw new Error((response && response.message) || fallbackMessage);
    }
    if (response.data === undefined || response.data === null) {
      throw new Error('API returned no data: ' + (response.message || fallbackMessage));
    }
    return response.data;
  }
}
