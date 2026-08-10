import { HttpClient } from '@angular/common/http';
import { Injectable, computed, inject, signal } from '@angular/core';
import { Observable, tap } from 'rxjs';
import { environment } from '../../../environments/environment';
import { ApiResponse } from '../models/api-response.model';
import {
  AuthResponse,
  ChangePasswordRequest,
  LoginRequest,
  RegisterRequest,
} from '../models/auth.model';
import { UserResponse } from '../models/user.model';
import { TokenStorageService } from './token-storage.service';

@Injectable({ providedIn: 'root' })
export class AuthService {
  private http = inject(HttpClient);
  private storage = inject(TokenStorageService);
  private base = `${environment.apiUrl}/auth`;

  /** Current authenticated user (null when logged out). */
  readonly currentUser = signal<UserResponse | null>(this.storage.getUser());
  readonly isAuthenticated = computed(() => !!this.currentUser());
  readonly roles = computed(() => this.currentUser()?.roles ?? []);

  login(payload: LoginRequest): Observable<ApiResponse<AuthResponse>> {
    return this.http
      .post<ApiResponse<AuthResponse>>(`${this.base}/login`, payload)
      .pipe(tap((res) => this.handleAuth(res.data)));
  }

  register(payload: RegisterRequest, collegeId?: File | null, profilePhoto?: File | null): Observable<ApiResponse<UserResponse>> {
    const formData = new FormData();
    formData.append('request', new Blob([JSON.stringify(payload)], { type: 'application/json' }));
    if (collegeId) {
      formData.append('collegeId', collegeId);
    }
    if (profilePhoto) {
      formData.append('profilePhoto', profilePhoto);
    }
    return this.http.post<ApiResponse<UserResponse>>(`${this.base}/register`, formData);
  }

  /** Used by the auth interceptor to rotate tokens on a 401. */
  refresh(): Observable<ApiResponse<AuthResponse>> {
    const refreshToken = this.storage.refreshToken;
    return this.http
      .post<ApiResponse<AuthResponse>>(`${this.base}/refresh`, { refreshToken })
      .pipe(tap((res) => this.handleAuth(res.data)));
  }

  changePassword(payload: ChangePasswordRequest): Observable<ApiResponse<void>> {
    return this.http.post<ApiResponse<void>>(`${this.base}/change-password`, payload);
  }

  logout(): Observable<ApiResponse<void>> | null {
    const refreshToken = this.storage.refreshToken;
    const obs = refreshToken
      ? this.http.post<ApiResponse<void>>(`${this.base}/logout`, { refreshToken })
      : null;
    this.clearSession();
    return obs;
  }

  clearSession(): void {
    this.storage.clear();
    this.currentUser.set(null);
  }

  hasAnyRole(roles: string[]): boolean {
    if (!roles?.length) return true;
    const mine = this.roles();
    return roles.some((r) => mine.includes(r));
  }

  private handleAuth(auth: AuthResponse): void {
    this.storage.setTokens(auth.accessToken, auth.refreshToken);
    this.storage.setUser(auth.user);
    this.currentUser.set(auth.user);
  }
}
