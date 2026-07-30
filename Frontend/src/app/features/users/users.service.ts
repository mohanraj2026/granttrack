import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { ApiResponse, PageResponse, PageQuery } from '../../core/models/api-response.model';
import { AdminCreateUserRequest, AdminUpdateUserRequest, CreatedUserResponse, UserResponse } from '../../core/models/user.model';
import { UserStatus } from '../../core/models/enums';
import { toHttpParams } from '../../core/utils/http-params.util';

@Injectable({ providedIn: 'root' })
export class UserAdminService {
  private http = inject(HttpClient);
  private base = `${environment.apiUrl}/users`;

  list(query?: PageQuery & { role?: string, status?: string }): Observable<ApiResponse<PageResponse<UserResponse>>> {
    return this.http.get<ApiResponse<PageResponse<UserResponse>>>(this.base, {
      params: toHttpParams(query),
    });
  }
  get(id: number): Observable<ApiResponse<UserResponse>> {
    return this.http.get<ApiResponse<UserResponse>>(`${this.base}/${id}`);
  }
  lookupResearchers(q?: string): Observable<ApiResponse<UserResponse[]>> {
    return this.http.get<ApiResponse<UserResponse[]>>(`${this.base}/lookup`, {
      params: q ? { q } : {}
    });
  }
  setStatus(id: number, status: UserStatus): Observable<ApiResponse<UserResponse>> {
    return this.http.patch<ApiResponse<UserResponse>>(`${this.base}/${id}/status`, null, {
      params: toHttpParams({ status }),
    });
  }
  deleteUser(id: number): Observable<ApiResponse<void>> {
    return this.http.delete<ApiResponse<void>>(`${this.base}/${id}`);
  }
  createUser(body: AdminCreateUserRequest): Observable<ApiResponse<CreatedUserResponse>> {
    return this.http.post<ApiResponse<CreatedUserResponse>>(this.base, body);
  }
  updateUser(id: number, body: AdminUpdateUserRequest): Observable<ApiResponse<UserResponse>> {
    return this.http.put<ApiResponse<UserResponse>>(`${this.base}/${id}`, body);
  }
}
