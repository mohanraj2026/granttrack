import {
  AssignmentStatus,
  AwardDecision,
  ConflictScreeningStatus,
  OverallRecommendation,
  ReviewCriterion,
} from './enums';

export interface ReviewerAssignmentRequest {
  applicationId: number;
  reviewerId: number;
  reviewDeadline?: string;
}
export interface ReviewerAssignmentResponse {
  id: number;
  applicationId: number;
  reviewerId: number;
  assignedDate: string;
  reviewDeadline?: string;
  conflictScreeningStatus: ConflictScreeningStatus;
  status: AssignmentStatus;
  responseComment?: string;
}

export interface ReviewScoreRequest {
  criterion: ReviewCriterion;
  score: number;
  comments?: string;
  overallRecommendation?: OverallRecommendation;
}
export interface ReviewScoreResponse {
  id: number;
  assignmentId: number;
  criterion: ReviewCriterion;
  score: number;
  comments?: string;
  overallRecommendation?: OverallRecommendation;
  submittedDate?: string;
}

export interface PanelDecisionRequest {
  panelDate?: string;
  consensusScore?: number;
  awardDecision: AwardDecision;
  awardedAmount?: number;
  conditionsAttached?: string;
  financeOfficerId?: number | null;
}
export interface PanelDecisionResponse {
  id: number;
  applicationId: number;
  panelDate?: string;
  consensusScore?: number;
  awardDecision: AwardDecision;
  awardedAmount?: number;
  conditionsAttached?: string;
  decidedById?: number;
  financeOfficerId?: number;
}
