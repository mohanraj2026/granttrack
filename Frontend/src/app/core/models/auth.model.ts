import { UserResponse } from './user.model';

export interface LoginRequest {
  email: string;
  password: string;
}

export interface RegisterRequest {
  name: string;
  email: string;
  password: string;
  /** Exactly 10 digits — see phoneValidators. */
  phone: string;
  countryCode?: string;
  institutionId?: number;
  department?: string;
  education?: string;
  // Role is fixed to ROLE_RESEARCHER on the server; it is never client-supplied.
}

export interface RefreshTokenRequest {
  refreshToken: string;
}

export interface ChangePasswordRequest {
  currentPassword: string;
  newPassword: string;
}

export interface ForgotPasswordRequest {
  email: string;
}

export interface AuthResponse {
  accessToken: string;
  refreshToken: string;
  tokenType: string;
  expiresInMs: number;
  user: UserResponse;
}
