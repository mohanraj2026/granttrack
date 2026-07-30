import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { ApiResponse, PageResponse, PageQuery } from '../../core/models/api-response.model';
import {
  DeliverableRequest,
  DeliverableResponse,
  ProgressReportRequest,
  ProgressReportResponse,
} from '../../core/models/progress.model';
import { toHttpParams } from '../../core/utils/http-params.util';

@Injectable({ providedIn: 'root' })
export class ProgressService {
  private http = inject(HttpClient);
  private base = `${environment.apiUrl}/progress`;

  // ---- Reports ----
  listReports(query: PageQuery): Observable<ApiResponse<PageResponse<ProgressReportResponse>>> {
    return this.http.get<ApiResponse<PageResponse<ProgressReportResponse>>>(
      `${this.base}/reports`,
      { params: toHttpParams(query) },
    );
  }
  createReport(body: ProgressReportRequest): Observable<ApiResponse<ProgressReportResponse>> {
    return this.http.post<ApiResponse<ProgressReportResponse>>(`${this.base}/reports`, body);
  }
  updateReport(id: number, body: ProgressReportRequest): Observable<ApiResponse<ProgressReportResponse>> {
    return this.http.put<ApiResponse<ProgressReportResponse>>(`${this.base}/reports/${id}`, body);
  }
  submitReport(id: number): Observable<ApiResponse<ProgressReportResponse>> {
    return this.http.post<ApiResponse<ProgressReportResponse>>(`${this.base}/reports/${id}/submit`, null);
  }
  reviewReport(id: number, decision: 'APPROVE' | 'REQUEST_REVISION', comment?: string): Observable<ApiResponse<ProgressReportResponse>> {
    return this.http.post<ApiResponse<ProgressReportResponse>>(
      `${this.base}/reports/${id}/review`,
      null,
      { params: toHttpParams({ decision, comment: comment || undefined }) },
    );
  }
  uploadReportDocument(id: number, file: File): Observable<ApiResponse<ProgressReportResponse>> {
    const form = new FormData();
    form.append('file', file);
    return this.http.post<ApiResponse<ProgressReportResponse>>(`${this.base}/reports/${id}/document`, form);
  }
  downloadReportDocument(id: number): Observable<Blob> {
    return this.http.get(`${this.base}/reports/${id}/document`, { responseType: 'blob' });
  }

  // ---- Deliverables ----
  listDeliverables(query: PageQuery): Observable<ApiResponse<PageResponse<DeliverableResponse>>> {
    return this.http.get<ApiResponse<PageResponse<DeliverableResponse>>>(
      `${this.base}/deliverables`,
      { params: toHttpParams(query) },
    );
  }
  createDeliverable(body: DeliverableRequest): Observable<ApiResponse<DeliverableResponse>> {
    return this.http.post<ApiResponse<DeliverableResponse>>(`${this.base}/deliverables`, body);
  }
  uploadDeliverable(id: number, file: File): Observable<ApiResponse<DeliverableResponse>> {
    const form = new FormData();
    form.append('file', file);
    return this.http.post<ApiResponse<DeliverableResponse>>(`${this.base}/deliverables/${id}/upload`, form);
  }
  downloadDeliverable(id: number): Observable<Blob> {
    return this.http.get(`${this.base}/deliverables/${id}/document`, { responseType: 'blob' });
  }
  reviewDeliverable(id: number, decision: 'ACCEPT' | 'REJECT', comment?: string): Observable<ApiResponse<DeliverableResponse>> {
    return this.http.post<ApiResponse<DeliverableResponse>>(
      `${this.base}/deliverables/${id}/review`,
      null,
      { params: toHttpParams({ decision, comment: comment || undefined }) },
    );
  }
}
