import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { ApiResponse, PageResponse, PageQuery } from '../../core/models/api-response.model';
import {
  FundingSchemeRequest,
  FundingSchemeResponse,
  GrantCallRequest,
  GrantCallResponse,
  InstitutionRequest,
  InstitutionResponse,
  SponsorRequest,
  SponsorResponse,
} from '../../core/models/funding.model';
import { toHttpParams } from '../../core/utils/http-params.util';

@Injectable({ providedIn: 'root' })
export class FundingService {
  private http = inject(HttpClient);
  private base = `${environment.apiUrl}/funding`;

  // ---- Schemes ----
  listSchemes(query: PageQuery): Observable<ApiResponse<PageResponse<FundingSchemeResponse>>> {
    return this.http.get<ApiResponse<PageResponse<FundingSchemeResponse>>>(`${this.base}/schemes`, {
      params: toHttpParams(query),
    });
  }
  getScheme(id: number): Observable<ApiResponse<FundingSchemeResponse>> {
    return this.http.get<ApiResponse<FundingSchemeResponse>>(`${this.base}/schemes/${id}`);
  }
  createScheme(body: FundingSchemeRequest): Observable<ApiResponse<FundingSchemeResponse>> {
    return this.http.post<ApiResponse<FundingSchemeResponse>>(`${this.base}/schemes`, body);
  }
  updateScheme(id: number, body: FundingSchemeRequest): Observable<ApiResponse<FundingSchemeResponse>> {
    return this.http.put<ApiResponse<FundingSchemeResponse>>(`${this.base}/schemes/${id}`, body);
  }
  changeSchemeStatus(id: number, status: string): Observable<ApiResponse<FundingSchemeResponse>> {
    return this.http.patch<ApiResponse<FundingSchemeResponse>>(
      `${this.base}/schemes/${id}/status`,
      null,
      { params: toHttpParams({ status }) },
    );
  }
  uploadSchemeDocument(id: number, file: File): Observable<ApiResponse<FundingSchemeResponse>> {
    const formData = new FormData();
    formData.append('file', file);
    return this.http.post<ApiResponse<FundingSchemeResponse>>(`${this.base}/schemes/${id}/document`, formData);
  }
  downloadSchemeDocument(id: number): Observable<Blob> {
    return this.http.get(`${this.base}/schemes/${id}/document`, { responseType: 'blob' });
  }
  deleteScheme(id: number): Observable<ApiResponse<void>> {
    return this.http.delete<ApiResponse<void>>(`${this.base}/schemes/${id}`);
  }

  // ---- Calls ----
  listCalls(query: PageQuery): Observable<ApiResponse<PageResponse<GrantCallResponse>>> {
    return this.http.get<ApiResponse<PageResponse<GrantCallResponse>>>(`${this.base}/calls`, {
      params: toHttpParams(query),
    });
  }
  getCall(id: number): Observable<ApiResponse<GrantCallResponse>> {
    return this.http.get<ApiResponse<GrantCallResponse>>(`${this.base}/calls/${id}`);
  }
  createCall(body: GrantCallRequest): Observable<ApiResponse<GrantCallResponse>> {
    return this.http.post<ApiResponse<GrantCallResponse>>(`${this.base}/calls`, body);
  }
  updateCall(id: number, body: GrantCallRequest): Observable<ApiResponse<GrantCallResponse>> {
    return this.http.put<ApiResponse<GrantCallResponse>>(`${this.base}/calls/${id}`, body);
  }
  openCall(id: number): Observable<ApiResponse<GrantCallResponse>> {
    return this.http.post<ApiResponse<GrantCallResponse>>(`${this.base}/calls/${id}/open`, null);
  }
  closeCall(id: number): Observable<ApiResponse<GrantCallResponse>> {
    return this.http.post<ApiResponse<GrantCallResponse>>(`${this.base}/calls/${id}/close`, null);
  }
  terminateCall(id: number): Observable<ApiResponse<GrantCallResponse>> {
    return this.http.post<ApiResponse<GrantCallResponse>>(`${this.base}/calls/${id}/terminate`, null);
  }
  deleteCall(id: number): Observable<ApiResponse<void>> {
    return this.http.delete<ApiResponse<void>>(`${this.base}/calls/${id}`);
  }

  // ---- Sponsors ----
  listSponsors(query: PageQuery): Observable<ApiResponse<PageResponse<SponsorResponse>>> {
    return this.http.get<ApiResponse<PageResponse<SponsorResponse>>>(`${this.base}/sponsors`, {
      params: toHttpParams(query),
    });
  }
  createSponsor(body: SponsorRequest): Observable<ApiResponse<SponsorResponse>> {
    return this.http.post<ApiResponse<SponsorResponse>>(`${this.base}/sponsors`, body);
  }
  updateSponsor(id: number, body: SponsorRequest): Observable<ApiResponse<SponsorResponse>> {
    return this.http.put<ApiResponse<SponsorResponse>>(`${this.base}/sponsors/${id}`, body);
  }
  deleteSponsor(id: number): Observable<ApiResponse<void>> {
    return this.http.delete<ApiResponse<void>>(`${this.base}/sponsors/${id}`);
  }

  // ---- Institutions ----
  listInstitutions(query: PageQuery): Observable<ApiResponse<PageResponse<InstitutionResponse>>> {
    return this.http.get<ApiResponse<PageResponse<InstitutionResponse>>>(
      `${this.base}/institutions`,
      { params: toHttpParams(query) },
    );
  }
  createInstitution(body: InstitutionRequest): Observable<ApiResponse<InstitutionResponse>> {
    return this.http.post<ApiResponse<InstitutionResponse>>(`${this.base}/institutions`, body);
  }
  updateInstitution(id: number, body: InstitutionRequest): Observable<ApiResponse<InstitutionResponse>> {
    return this.http.put<ApiResponse<InstitutionResponse>>(`${this.base}/institutions/${id}`, body);
  }
  deleteInstitution(id: number): Observable<ApiResponse<void>> {
    return this.http.delete<ApiResponse<void>>(`${this.base}/institutions/${id}`);
  }
}
