import { DeliverableStatus, DeliverableType, ProgressStatus } from './enums';

export interface ProgressReportRequest {
  awardId: number;
  period?: string;
  summary?: string;
  keyAchievements?: string;
  challenges?: string;
  budgetUtilisationPercent?: number;
}
export interface ProgressReportResponse {
  id: number;
  awardId: number;
  period?: string;
  summary?: string;
  keyAchievements?: string;
  challenges?: string;
  budgetUtilisationPercent?: number;
  submittedById?: number;
  submittedDate?: string;
  reportDocName?: string;
  hasReportDocument?: boolean;
  reviewComment?: string;
  status: ProgressStatus;
  createdAt?: string;
  updatedAt?: string;
}

export interface DeliverableRequest {
  awardId: number;
  title: string;
  type: DeliverableType;
  dueDate?: string;
}
export interface DeliverableResponse {
  id: number;
  awardId: number;
  title: string;
  type: DeliverableType;
  dueDate?: string;
  submittedDate?: string;
  filePath?: string;
  fileName?: string;
  hasFile?: boolean;
  reviewComment?: string;
  status: DeliverableStatus;
  createdAt?: string;
  updatedAt?: string;
}
