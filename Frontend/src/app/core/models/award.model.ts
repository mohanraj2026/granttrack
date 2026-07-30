import { AwardStatus } from './enums';

export interface GrantAwardRequest {
  applicationId: number;
  awardedAmount: number;
  startDate?: string;
  endDate?: string;
  conditionsRef?: string;
}
export interface AwardTermsRequest {
  awardedAmount: number;
  startDate?: string;
  endDate?: string;
  conditionsRef?: string;
}
export interface GrantAwardResponse {
  id: number;
  applicationId: number;
  awardedAmount: number;
  startDate?: string;
  endDate?: string;
  conditionsRef?: string;
  awardLetterDate?: string;
  status: AwardStatus;
  financeOfficerId?: number;
  financeReviewStatus?: 'PENDING' | 'ACCEPTED' | 'REJECTED';
  financeReviewComment?: string;
  createdAt?: string;
  updatedAt?: string;
}
