import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable, map } from 'rxjs';
import { environment } from '../../../environments/environment';
import { ApiResponse } from '../../shared/models/api-response.model';
import {
  AmbassadorApplicationDetail,
  AmbassadorApplicationPageResponse,
  RejectApplicationRequest
} from '../../shared/models/ambassador-application.model';

// Company-admin review surface for pending ambassador applications (see
// AmbassadorApplicationAdminController) - requires COMPANY_ADMIN, unlike
// AmbassadorApplicationService's public apply().
@Injectable({
  providedIn: 'root'
})
export class AmbassadorApplicationAdminService {
  constructor(private http: HttpClient) {}

  listApplications(params: {
    page?: number;
    size?: number;
    sort?: string;
    search?: string;
    status?: string;
  }): Observable<AmbassadorApplicationPageResponse> {
    let httpParams = new HttpParams()
      .set('page', String(params.page ?? 0))
      .set('size', String(params.size ?? 20));

    if (params.sort) {
      httpParams = httpParams.set('sort', params.sort);
    }
    if (params.search) {
      httpParams = httpParams.set('search', params.search);
    }
    if (params.status) {
      httpParams = httpParams.set('status', params.status);
    }

    return this.http
      .get<ApiResponse<AmbassadorApplicationPageResponse>>(`${environment.apiUrl}/admin/ambassador-applications`, { params: httpParams })
      .pipe(map(response => this.unwrapResponse(response, 'Unable to load ambassador applications.')));
  }

  approveApplication(applicationId: number): Observable<void> {
    return this.http
      .post<ApiResponse<unknown>>(`${environment.apiUrl}/admin/ambassador-applications/${applicationId}/approve`, {})
      .pipe(map(() => void 0));
  }

  rejectApplication(applicationId: number, request: RejectApplicationRequest): Observable<AmbassadorApplicationDetail> {
    return this.http
      .post<ApiResponse<AmbassadorApplicationDetail>>(`${environment.apiUrl}/admin/ambassador-applications/${applicationId}/reject`, request)
      .pipe(map(response => this.unwrapResponse(response, 'Unable to reject application.')));
  }

  private unwrapResponse<T>(response: ApiResponse<T>, fallbackMessage: string): T {
    if (!response.success || response.data === undefined || response.data === null) {
      throw new Error(response.message || fallbackMessage);
    }

    return response.data;
  }
}
