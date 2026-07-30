import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { ApiResponse, PageResponse, PageQuery } from '../../core/models/api-response.model';
import {
  AwardTermsRequest,
  GrantAwardRequest,
  GrantAwardResponse,
} from '../../core/models/award.model';
import { toHttpParams } from '../../core/utils/http-params.util';

@Injectable({ providedIn: 'root' })
export class AwardService {
  private http = inject(HttpClient);
  private base = `${environment.apiUrl}/awards`;

  list(query: PageQuery & { financeOfficerId?: number; financeReviewStatus?: string }): Observable<ApiResponse<PageResponse<GrantAwardResponse>>> {
    return this.http.get<ApiResponse<PageResponse<GrantAwardResponse>>>(this.base, {
      params: toHttpParams(query),
    });
  }
  /** Assigned finance officer accepts or rejects an award for disbursement. */
  financeReview(id: number, decision: 'ACCEPT' | 'REJECT', reason?: string): Observable<ApiResponse<GrantAwardResponse>> {
    return this.http.post<ApiResponse<GrantAwardResponse>>(`${this.base}/${id}/finance-review`, null, {
      params: toHttpParams({ decision, reason }),
    });
  }
  get(id: number): Observable<ApiResponse<GrantAwardResponse>> {
    return this.http.get<ApiResponse<GrantAwardResponse>>(`${this.base}/${id}`);
  }
  create(body: GrantAwardRequest): Observable<ApiResponse<GrantAwardResponse>> {
    return this.http.post<ApiResponse<GrantAwardResponse>>(this.base, body);
  }
  updateTerms(id: number, body: AwardTermsRequest): Observable<ApiResponse<GrantAwardResponse>> {
    return this.http.put<ApiResponse<GrantAwardResponse>>(`${this.base}/${id}`, body);
  }
  approve(id: number): Observable<ApiResponse<GrantAwardResponse>> {
    return this.http.post<ApiResponse<GrantAwardResponse>>(`${this.base}/${id}/approve`, null);
  }
  changeStatus(id: number, status: string): Observable<ApiResponse<GrantAwardResponse>> {
    return this.http.patch<ApiResponse<GrantAwardResponse>>(`${this.base}/${id}/status`, null, {
      params: toHttpParams({ status }),
    });
  }
}
