import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { ApiResponse, PageResponse, PageQuery } from '../../core/models/api-response.model';
import {
  PanelDecisionRequest,
  PanelDecisionResponse,
  ReviewScoreRequest,
  ReviewScoreResponse,
  ReviewerAssignmentRequest,
  ReviewerAssignmentResponse,
} from '../../core/models/review.model';
import { BlindApplicationResponse } from '../../core/models/application.model';
import { toHttpParams } from '../../core/utils/http-params.util';

@Injectable({ providedIn: 'root' })
export class ReviewService {
  private http = inject(HttpClient);
  private base = `${environment.apiUrl}/reviews`;

  // ---- Assignments ----
  listAssignments(query: PageQuery): Observable<ApiResponse<PageResponse<ReviewerAssignmentResponse>>> {
    return this.http.get<ApiResponse<PageResponse<ReviewerAssignmentResponse>>>(
      `${this.base}/assignments`,
      { params: toHttpParams(query) },
    );
  }
  createAssignment(body: ReviewerAssignmentRequest): Observable<ApiResponse<ReviewerAssignmentResponse>> {
    return this.http.post<ApiResponse<ReviewerAssignmentResponse>>(`${this.base}/assignments`, body);
  }
  conflictCheck(id: number, status: string): Observable<ApiResponse<ReviewerAssignmentResponse>> {
    return this.http.post<ApiResponse<ReviewerAssignmentResponse>>(
      `${this.base}/assignments/${id}/conflict-check`,
      null,
      { params: toHttpParams({ status }) },
    );
  }
  respond(id: number, decision: 'ACCEPT' | 'DECLINE', reason?: string): Observable<ApiResponse<ReviewerAssignmentResponse>> {
    return this.http.post<ApiResponse<ReviewerAssignmentResponse>>(
      `${this.base}/assignments/${id}/respond`,
      null,
      { params: toHttpParams({ decision, reason }) },
    );
  }
  submitScore(id: number, body: ReviewScoreRequest): Observable<ApiResponse<ReviewScoreResponse>> {
    return this.http.post<ApiResponse<ReviewScoreResponse>>(
      `${this.base}/assignments/${id}/scores`,
      body,
    );
  }
  submitReview(id: number): Observable<ApiResponse<ReviewerAssignmentResponse>> {
    return this.http.post<ApiResponse<ReviewerAssignmentResponse>>(
      `${this.base}/assignments/${id}/submit`,
      null,
    );
  }
  listScores(id: number): Observable<ApiResponse<ReviewScoreResponse[]>> {
    return this.http.get<ApiResponse<ReviewScoreResponse[]>>(`${this.base}/assignments/${id}/scores`);
  }
  /** All submitted review scores for an application (Grant Admin — read before the panel decision). */
  listApplicationReviews(appId: number): Observable<ApiResponse<ReviewScoreResponse[]>> {
    return this.http.get<ApiResponse<ReviewScoreResponse[]>>(`${this.base}/applications/${appId}/reviews`);
  }

  getBlindApplication(appId: number): Observable<ApiResponse<BlindApplicationResponse>> {
    return this.http.get<ApiResponse<BlindApplicationResponse>>(
      `${environment.apiUrl}/applications/${appId}/blind`,
    );
  }

  // ---- Panel decisions ----
  getPanelDecision(appId: number): Observable<ApiResponse<PanelDecisionResponse>> {
    return this.http.get<ApiResponse<PanelDecisionResponse>>(
      `${this.base}/applications/${appId}/panel-decision`,
    );
  }
  createPanelDecision(
    appId: number,
    body: PanelDecisionRequest,
  ): Observable<ApiResponse<PanelDecisionResponse>> {
    return this.http.post<ApiResponse<PanelDecisionResponse>>(
      `${this.base}/applications/${appId}/panel-decision`,
      body,
    );
  }
  updatePanelDecision(
    appId: number,
    body: PanelDecisionRequest,
  ): Observable<ApiResponse<PanelDecisionResponse>> {
    return this.http.put<ApiResponse<PanelDecisionResponse>>(
      `${this.base}/applications/${appId}/panel-decision`,
      body,
    );
  }
  listPanelDecisions(query: PageQuery): Observable<ApiResponse<PageResponse<PanelDecisionResponse>>> {
    return this.http.get<ApiResponse<PageResponse<PanelDecisionResponse>>>(
      `${this.base}/panel-decisions`,
      { params: toHttpParams(query) },
    );
  }
}
