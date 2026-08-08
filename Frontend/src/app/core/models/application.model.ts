import {
  ApplicationStatus,
  BudgetHead,
  CoInvestigatorRole,
  CoInvestigatorStatus,
} from './enums';

export interface GrantApplicationRequest {
  callId: number;
  projectTitle: string;
  researchAbstract?: string;
  discipline?: string;
  requestedAmount: number;
  projectDurationMonths?: number;
  institutionId?: number;
}
export interface GrantApplicationResponse {
  id: number;
  callId: number;
  principalInvestigatorId: number;
  projectTitle: string;
  researchAbstract?: string;
  discipline?: string;
  requestedAmount: number;
  projectDurationMonths?: number;
  institutionId?: number;
  submissionDate?: string;
  abstractDocPath?: string;
  abstractDocName?: string;
  status: ApplicationStatus;
  createdAt?: string;
  updatedAt?: string;
}

export interface BlindApplicationResponse {
  id: number;
  callId: number;
  projectTitle: string;
  researchAbstract?: string;
  discipline?: string;
  requestedAmount: number;
  projectDurationMonths?: number;
  abstractDocName?: string;
}

export interface CoInvestigatorRequest {
  userId?: number;
  institutionId?: number;
  role: CoInvestigatorRole;
  contribution?: string;
  status?: CoInvestigatorStatus;
}
export interface CoInvestigatorResponse {
  id: number;
  applicationId: number;
  userId?: number;
  institutionId?: number;
  role: CoInvestigatorRole;
  contribution?: string;
  status: CoInvestigatorStatus;
}

/** A co-investigator invitation addressed to the current user (with the application's title). */
export interface MyInvitationResponse {
  coInvestigatorId: number;
  applicationId: number;
  projectTitle: string;
  role: string;
  status: string;
}

export interface ApplicationBudgetRequest {
  budgetHead: BudgetHead;
  amount: number;
  justification?: string;
}
export interface ApplicationBudgetResponse {
  id: number;
  applicationId: number;
  budgetHead: BudgetHead;
  amount: number;
  justification?: string;
}
