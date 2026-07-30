/** Standard backend response envelope: { success, message, data, timestamp }. */
export interface ApiResponse<T> {
  success: boolean;
  message: string;
  data: T;
  timestamp: string;
}

/** Serialized Spring Data page placed inside ApiResponse.data for list endpoints. */
export interface PageResponse<T> {
  content: T[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
  last: boolean;
}

export interface FieldValidationError {
  field: string;
  message: string;
  rejectedValue?: unknown;
}

export interface ErrorResponse {
  status: number;
  error: string;
  path: string;
  fieldErrors?: FieldValidationError[];
}

/** Common query params for paginated/searchable list endpoints. */
export interface PageQuery {
  page?: number;
  size?: number;
  sort?: string; // e.g. "createdAt,desc"
  [key: string]: string | number | boolean | undefined;
}
