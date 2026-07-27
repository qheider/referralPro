import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable, map } from 'rxjs';
import { environment } from '../../../environments/environment';
import { ApiResponse } from '../../shared/models/api-response.model';
import {
  AmbassadorDetail,
  AmbassadorPageResponse,
  CreateAmbassadorRequest,
  UpdateAmbassadorRequest
} from '../../shared/models/ambassador.model';

@Injectable({
  providedIn: 'root'
})
export class AmbassadorAdminService {
  constructor(private http: HttpClient) {}

  listAmbassadors(params: {
    page?: number;
    size?: number;
    sort?: string;
    search?: string;
    status?: string;
  }): Observable<AmbassadorPageResponse> {
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
      .get<ApiResponse<AmbassadorPageResponse>>(`${environment.apiUrl}/admin/ambassadors`, { params: httpParams })
      .pipe(map(response => this.unwrapResponse(response, 'Unable to load ambassadors.')));
  }

  getAmbassador(ambassadorId: number): Observable<AmbassadorDetail> {
    return this.http
      .get<ApiResponse<AmbassadorDetail>>(`${environment.apiUrl}/admin/ambassadors/${ambassadorId}`)
      .pipe(map(response => this.unwrapResponse(response, 'Unable to load ambassador details.')));
  }

  createAmbassador(request: CreateAmbassadorRequest): Observable<void> {
    return this.http
      .post<ApiResponse<unknown>>(`${environment.apiUrl}/admin/ambassadors`, request)
      .pipe(map(() => void 0));
  }

  updateAmbassador(ambassadorId: number, request: UpdateAmbassadorRequest): Observable<AmbassadorDetail> {
    return this.http
      .put<ApiResponse<AmbassadorDetail>>(`${environment.apiUrl}/admin/ambassadors/${ambassadorId}`, request)
      .pipe(map(response => this.unwrapResponse(response, 'Unable to update ambassador.')));
  }

  activateAmbassador(ambassadorId: number): Observable<AmbassadorDetail> {
    return this.http
      .patch<ApiResponse<AmbassadorDetail>>(`${environment.apiUrl}/admin/ambassadors/${ambassadorId}/activate`, {})
      .pipe(map(response => this.unwrapResponse(response, 'Unable to activate ambassador.')));
  }

  deactivateAmbassador(ambassadorId: number): Observable<AmbassadorDetail> {
    return this.http
      .patch<ApiResponse<AmbassadorDetail>>(`${environment.apiUrl}/admin/ambassadors/${ambassadorId}/deactivate`, {})
      .pipe(map(response => this.unwrapResponse(response, 'Unable to deactivate ambassador.')));
  }

  private unwrapResponse<T>(response: ApiResponse<T>, fallbackMessage: string): T {
    if (!response.success || response.data === undefined) {
      throw new Error(response.message || fallbackMessage);
    }

    return response.data;
  }
}
