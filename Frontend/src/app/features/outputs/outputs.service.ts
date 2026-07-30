import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { ApiResponse, PageResponse, PageQuery } from '../../core/models/api-response.model';
import {
  IpRecordRequest,
  IpRecordResponse,
  ResearchOutputRequest,
  ResearchOutputResponse,
} from '../../core/models/output.model';
import { toHttpParams } from '../../core/utils/http-params.util';

@Injectable({ providedIn: 'root' })
export class OutputService {
  private http = inject(HttpClient);
  private base = `${environment.apiUrl}/outputs`;

  // ---- Research Outputs ----
  listOutputs(query: PageQuery): Observable<ApiResponse<PageResponse<ResearchOutputResponse>>> {
    return this.http.get<ApiResponse<PageResponse<ResearchOutputResponse>>>(`${this.base}`, {
      params: toHttpParams(query),
    });
  }
  getOutput(id: number): Observable<ApiResponse<ResearchOutputResponse>> {
    return this.http.get<ApiResponse<ResearchOutputResponse>>(`${this.base}/${id}`);
  }
  createOutput(body: ResearchOutputRequest): Observable<ApiResponse<ResearchOutputResponse>> {
    return this.http.post<ApiResponse<ResearchOutputResponse>>(`${this.base}`, body);
  }
  updateOutput(id: number, body: ResearchOutputRequest): Observable<ApiResponse<ResearchOutputResponse>> {
    return this.http.put<ApiResponse<ResearchOutputResponse>>(`${this.base}/${id}`, body);
  }
  deleteOutput(id: number): Observable<ApiResponse<void>> {
    return this.http.delete<ApiResponse<void>>(`${this.base}/${id}`);
  }

  // ---- IP Records ----
  listIp(query: PageQuery): Observable<ApiResponse<PageResponse<IpRecordResponse>>> {
    return this.http.get<ApiResponse<PageResponse<IpRecordResponse>>>(`${this.base}/ip`, {
      params: toHttpParams(query),
    });
  }
  createIp(body: IpRecordRequest): Observable<ApiResponse<IpRecordResponse>> {
    return this.http.post<ApiResponse<IpRecordResponse>>(`${this.base}/ip`, body);
  }
  updateIp(id: number, body: IpRecordRequest): Observable<ApiResponse<IpRecordResponse>> {
    return this.http.put<ApiResponse<IpRecordResponse>>(`${this.base}/ip/${id}`, body);
  }
  deleteIp(id: number): Observable<ApiResponse<void>> {
    return this.http.delete<ApiResponse<void>>(`${this.base}/ip/${id}`);
  }
}
