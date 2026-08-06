import { UserStatus } from './enums';

export interface UserResponse {
  id: number;
  formattedId?: string;
  name: string;
  email: string;
  phone?: string;
  countryCode?: string;
  institutionId?: number;
  department?: string;
  education?: string;
  collegeIdPath?: string;
  profilePhotoPath?: string;
  status: UserStatus;
  roles: string[];
  createdAt?: string;
  updatedAt?: string;
}

/** Admin provisioning of an operational user account. */
export interface AdminCreateUserRequest {
  name: string;
  email: string;
  /** Exactly 10 digits — see phoneValidators. */
  phone: string;
  institutionId?: number;
  department?: string;
  role: string;
  password: string;
}

/** Admin update of an existing user's editable details (no password / role change). */
export interface AdminUpdateUserRequest {
  name: string;
  email: string;
  /** Exactly 10 digits — see phoneValidators. */
  phone: string;
  institutionId?: number;
  department?: string;
}

/** Returned once on creation — temporary password for secure hand-off. */
export interface CreatedUserResponse {
  user: UserResponse;
}
