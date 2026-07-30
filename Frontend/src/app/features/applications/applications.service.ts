import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { ApiResponse, PageResponse, PageQuery } from '../../core/models/api-response.model';
import {
  ApplicationBudgetRequest,
  ApplicationBudgetResponse,
  CoInvestigatorRequest,
  CoInvestigatorResponse,
  GrantApplicationRequest,
  GrantApplicationResponse,
} from '../../core/models/application.model';
import { toHttpParams } from '../../core/utils/http-params.util';

@Injectable({ providedIn: 'root' })
export class ApplicationsService {
  private http = inject(HttpClient);
  private base = `${environment.apiUrl}/applications`;

  list(query: PageQuery): Observable<ApiResponse<PageResponse<GrantApplicationResponse>>> {
    return this.http.get<ApiResponse<PageResponse<GrantApplicationResponse>>>(this.base, {
      params: toHttpParams(query),
    });
  }
  get(id: number): Observable<ApiResponse<GrantApplicationResponse>> {
    return this.http.get<ApiResponse<GrantApplicationResponse>>(`${this.base}/${id}`);
  }
  create(body: GrantApplicationRequest): Observable<ApiResponse<GrantApplicationResponse>> {
    return this.http.post<ApiResponse<GrantApplicationResponse>>(this.base, body);
  }
  update(id: number, body: GrantApplicationRequest): Observable<ApiResponse<GrantApplicationResponse>> {
    return this.http.put<ApiResponse<GrantApplicationResponse>>(`${this.base}/${id}`, body);
  }
  submit(id: number): Observable<ApiResponse<GrantApplicationResponse>> {
    return this.http.post<ApiResponse<GrantApplicationResponse>>(`${this.base}/${id}/submit`, null);
  }
  withdraw(id: number): Observable<ApiResponse<GrantApplicationResponse>> {
    return this.http.post<ApiResponse<GrantApplicationResponse>>(`${this.base}/${id}/withdraw`, null);
  }
  changeStatus(id: number, status: string): Observable<ApiResponse<GrantApplicationResponse>> {
    return this.http.patch<ApiResponse<GrantApplicationResponse>>(`${this.base}/${id}/status`, null, {
      params: toHttpParams({ status }),
    });
  }

  // Co-investigators
  listCoInvestigators(appId: number): Observable<ApiResponse<CoInvestigatorResponse[]>> {
    return this.http.get<ApiResponse<CoInvestigatorResponse[]>>(`${this.base}/${appId}/co-investigators`);
  }
  addCoInvestigator(appId: number, body: CoInvestigatorRequest): Observable<ApiResponse<CoInvestigatorResponse>> {
    return this.http.post<ApiResponse<CoInvestigatorResponse>>(`${this.base}/${appId}/co-investigators`, body);
  }
  removeCoInvestigator(appId: number, coiId: number): Observable<ApiResponse<void>> {
    return this.http.delete<ApiResponse<void>>(`${this.base}/${appId}/co-investigators/${coiId}`);
  }
  /** The invited co-investigator accepts or declines their invitation. */
  respondCoInvestigator(
    appId: number,
    coiId: number,
    decision: 'ACCEPT' | 'DECLINE',
  ): Observable<ApiResponse<CoInvestigatorResponse>> {
    return this.http.post<ApiResponse<CoInvestigatorResponse>>(
      `${this.base}/${appId}/co-investigators/${coiId}/respond`,
      null,
      { params: toHttpParams({ decision }) },
    );
  }

  // Budgets
  listBudgets(appId: number): Observable<ApiResponse<ApplicationBudgetResponse[]>> {
    return this.http.get<ApiResponse<ApplicationBudgetResponse[]>>(`${this.base}/${appId}/budgets`);
  }
  addBudget(appId: number, body: ApplicationBudgetRequest): Observable<ApiResponse<ApplicationBudgetResponse>> {
    return this.http.post<ApiResponse<ApplicationBudgetResponse>>(`${this.base}/${appId}/budgets`, body);
  }
  removeBudget(appId: number, budgetId: number): Observable<ApiResponse<void>> {
    return this.http.delete<ApiResponse<void>>(`${this.base}/${appId}/budgets/${budgetId}`);
  }

  // Abstract document
  uploadAbstract(appId: number, file: File): Observable<ApiResponse<GrantApplicationResponse>> {
    const form = new FormData();
    form.append('file', file);
    return this.http.post<ApiResponse<GrantApplicationResponse>>(
      `${this.base}/${appId}/abstract-document`,
      form,
    );
  }
  abstractDownloadUrl(appId: number): string {
    return `${this.base}/${appId}/abstract-document`;
  }
  /** Authenticated blob download (the auth interceptor attaches the bearer token). */
  downloadAbstract(appId: number): Observable<Blob> {
    return this.http.get(`${this.base}/${appId}/abstract-document`, { responseType: 'blob' });
  }
}
