import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { ApiResponse, PageResponse, PageQuery } from '../../core/models/api-response.model';
import {
  FundDisbursementResponse,
  MilestoneRequest,
  MilestoneResponse,
  MilestoneUpdateRequest,
  ReleaseFundsRequest,
} from '../../core/models/disbursement.model';
import { toHttpParams } from '../../core/utils/http-params.util';

@Injectable({ providedIn: 'root' })
export class DisbursementService {
  private http = inject(HttpClient);
  private base = `${environment.apiUrl}/disbursements`;

  // ---- Milestones ----
  listMilestones(query: PageQuery): Observable<ApiResponse<PageResponse<MilestoneResponse>>> {
    return this.http.get<ApiResponse<PageResponse<MilestoneResponse>>>(
      `${this.base}/milestones`,
      { params: toHttpParams(query) },
    );
  }
  getMilestone(id: number): Observable<ApiResponse<MilestoneResponse>> {
    return this.http.get<ApiResponse<MilestoneResponse>>(`${this.base}/milestones/${id}`);
  }
  createMilestone(body: MilestoneRequest): Observable<ApiResponse<MilestoneResponse>> {
    return this.http.post<ApiResponse<MilestoneResponse>>(`${this.base}/milestones`, body);
  }
  updateMilestone(id: number, body: MilestoneUpdateRequest): Observable<ApiResponse<MilestoneResponse>> {
    return this.http.put<ApiResponse<MilestoneResponse>>(`${this.base}/milestones/${id}`, body);
  }
  /** Finance verifies a milestone whose progress report was approved by Compliance (-> COMPLETED). */
  verify(id: number): Observable<ApiResponse<MilestoneResponse>> {
    return this.http.post<ApiResponse<MilestoneResponse>>(
      `${this.base}/milestones/${id}/verify`,
      null,
    );
  }
  release(id: number, body: ReleaseFundsRequest): Observable<ApiResponse<FundDisbursementResponse>> {
    return this.http.post<ApiResponse<FundDisbursementResponse>>(
      `${this.base}/milestones/${id}/release`,
      body,
    );
  }

  // ---- Releases ----
  listReleases(query: PageQuery): Observable<ApiResponse<PageResponse<FundDisbursementResponse>>> {
    return this.http.get<ApiResponse<PageResponse<FundDisbursementResponse>>>(this.base, {
      params: toHttpParams(query),
    });
  }
}
