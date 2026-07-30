import { HttpClient } from '@angular/common/http';
import { Injectable, inject, signal } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { ApiResponse, PageResponse, PageQuery } from '../../core/models/api-response.model';
import { NotificationRequest, NotificationResponse } from '../../core/models/notification.model';
import { toHttpParams } from '../../core/utils/http-params.util';

@Injectable({ providedIn: 'root' })
export class NotificationService {
  private http = inject(HttpClient);
  private base = `${environment.apiUrl}/notifications`;

  /** Live unread count shared with the shell bell badge; kept fresh by refreshUnread(). */
  readonly unread = signal(0);

  list(query: PageQuery): Observable<ApiResponse<PageResponse<NotificationResponse>>> {
    return this.http.get<ApiResponse<PageResponse<NotificationResponse>>>(this.base, {
      params: toHttpParams(query),
    });
  }
  unreadCount(): Observable<ApiResponse<number>> {
    return this.http.get<ApiResponse<number>>(`${this.base}/unread-count`);
  }
  /** Fetch the unread count and update the shared signal (drives the bell badge). */
  refreshUnread(): void {
    this.unreadCount().subscribe({
      next: (r) => this.unread.set(r.data ?? 0),
      error: () => {},
    });
  }
  markRead(id: number): Observable<ApiResponse<NotificationResponse>> {
    return this.http.patch<ApiResponse<NotificationResponse>>(`${this.base}/${id}/read`, null);
  }
  dismiss(id: number): Observable<ApiResponse<NotificationResponse>> {
    return this.http.patch<ApiResponse<NotificationResponse>>(`${this.base}/${id}/dismiss`, null);
  }
  create(body: NotificationRequest): Observable<ApiResponse<NotificationResponse>> {
    return this.http.post<ApiResponse<NotificationResponse>>(this.base, body);
  }
}
