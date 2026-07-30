import { DisbursementStatus, MilestoneStatus } from './enums';

export interface MilestoneRequest {
  awardId: number;
  milestoneNumber: number;
  description?: string;
  dueDate?: string;
  amount: number;
  evidenceRequired?: boolean;
}
export interface MilestoneUpdateRequest {
  description?: string;
  dueDate?: string;
  amount: number;
  evidenceRequired?: boolean;
}
export interface MilestoneResponse {
  id: number;
  awardId: number;
  milestoneNumber: number;
  description?: string;
  dueDate?: string;
  amount: number;
  evidenceRequired: boolean;
  evidenceNote?: string;
  evidenceDocName?: string;
  hasEvidenceDocument?: boolean;
  evidenceSubmittedDate?: string;
  evidenceReviewComment?: string;
  status: MilestoneStatus;
  createdAt?: string;
  updatedAt?: string;
}

export interface ReleaseFundsRequest {
  receivingAccountRef?: string;
  paymentReference?: string;
  releaseDate?: string;
}
export interface FundDisbursementResponse {
  id: number;
  milestoneId: number;
  milestoneDescription?: string;
  awardId: number;
  amount: number;
  disbursedDate?: string;
  receivingAccountRef?: string;
  paymentReference?: string;
  status: DisbursementStatus;
  createdAt?: string;
}
