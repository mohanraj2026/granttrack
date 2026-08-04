/* Enums mirroring the backend, plus human-friendly label maps used across the UI. */

export enum Role {
  RESEARCHER = 'ROLE_RESEARCHER',
  REVIEWER = 'ROLE_REVIEWER',
  GRANT_ADMIN = 'ROLE_GRANT_ADMIN',
  FINANCE_OFFICER = 'ROLE_FINANCE_OFFICER',
  COMPLIANCE_OFFICER = 'ROLE_COMPLIANCE_OFFICER',
  ADMIN = 'ROLE_ADMIN',
}

export const ROLE_LABELS: Record<string, string> = {
  ROLE_RESEARCHER: 'Researcher',
  ROLE_REVIEWER: 'Reviewer',
  ROLE_GRANT_ADMIN: 'Grant Administrator',
  ROLE_FINANCE_OFFICER: 'Finance Officer',
  ROLE_COMPLIANCE_OFFICER: 'Compliance Officer',
  ROLE_ADMIN: 'System Admin',
};

export type UserStatus = 'ACTIVE' | 'INACTIVE';
export type SchemeStatus = 'ACTIVE' | 'CLOSED' | 'SUSPENDED';
export type ReviewMethod = 'DOUBLE_BLIND' | 'PANEL';
export type CallStatus = 'UPCOMING' | 'OPEN' | 'UNDER_REVIEW' | 'AWARDED' | 'CLOSED' | 'TERMINATED';
export type ApplicationStatus =
  | 'DRAFT'
  | 'SUBMITTED'
  | 'UNDER_REVIEW'
  | 'AWARDED'
  | 'DECLINED'
  | 'WITHDRAWN';
export type CoInvestigatorRole = 'CO_INVESTIGATOR' | 'RESEARCH_ASSISTANT' | 'INDUSTRIAL_PARTNER';
export type CoInvestigatorStatus = 'INVITED' | 'CONFIRMED' | 'DECLINED';
export type BudgetHead =
  | 'PERSONNEL'
  | 'EQUIPMENT'
  | 'TRAVEL'
  | 'CONSUMABLES'
  | 'OVERHEAD'
  | 'SUBCONTRACT';
export type ConflictScreeningStatus = 'CLEAR' | 'COI_DECLARED';
export type AssignmentStatus = 'ASSIGNED' | 'ACCEPTED' | 'DECLINED' | 'SUBMITTED';
export type ReviewCriterion =
  | 'SCIENTIFIC_MERIT'
  | 'FEASIBILITY'
  | 'TEAM_EXPERTISE'
  | 'IMPACT'
  | 'INNOVATION'
  | 'BUDGET_JUSTIFICATION';
export type OverallRecommendation = 'FUND_AT_FULL_AMOUNT' | 'FUND_AT_REDUCED' | 'DO_NOT_FUND';
export type AwardDecision = 'FULL_AWARD' | 'REDUCED_AWARD' | 'RESERVE_LIST' | 'REJECTED';
export type AwardStatus = 'ACTIVE' | 'SUSPENDED' | 'COMPLETED' | 'TERMINATED';
export type MilestoneStatus =
  | 'UPCOMING'
  | 'UNDER_REVIEW'
  | 'AWAITING_FINANCE_VERIFICATION'
  | 'REVISION_REQUESTED'
  | 'COMPLETED'
  | 'DISBURSED'
  | 'OVERDUE'
  // legacy states from the previous evidence flow (older rows)
  | 'EVIDENCE_SUBMITTED'
  | 'APPROVED';
export type DisbursementStatus = 'PENDING' | 'RELEASED' | 'FAILED';
export type ProgressStatus = 'DRAFT' | 'SUBMITTED' | 'APPROVED' | 'REVISION_REQUESTED';
export type DeliverableType =
  | 'REPORT'
  | 'DATASET'
  | 'PROTOTYPE'
  | 'PUBLICATION'
  | 'TRAINING'
  | 'POLICY';
export type DeliverableStatus = 'PENDING' | 'SUBMITTED' | 'ACCEPTED' | 'REJECTED';
export type OutputType =
  | 'JOURNAL_ARTICLE'
  | 'CONFERENCE_PAPER'
  | 'PATENT'
  | 'DATASET'
  | 'SOFTWARE'
  | 'POLICY_BRIEF';
export type OutputStatus = 'PUBLISHED' | 'SUBMITTED' | 'IN_PREPARATION';
export type IpType = 'PATENT' | 'COPYRIGHT' | 'TRADEMARK' | 'TRADE_SECRET';
export type IpStatus = 'FILED' | 'GRANTED' | 'ABANDONED';
export type NotificationCategory =
  | 'APPLICATION'
  | 'REVIEW'
  | 'AWARD'
  | 'DISBURSEMENT'
  | 'PROGRESS'
  | 'OUTPUT';
export type NotificationStatus = 'UNREAD' | 'READ' | 'DISMISSED';

/** Maps a status string to a shared badge variant. */
export type BadgeVariant = 'success' | 'warning' | 'danger' | 'info' | 'neutral';

export const STATUS_VARIANT: Record<string, BadgeVariant> = {
  // generic / active
  ACTIVE: 'success', OPEN: 'success', APPROVED: 'success', PUBLISHED: 'success',
  RELEASED: 'success', ACCEPTED: 'success', GRANTED: 'success', CONFIRMED: 'success',
  AWARDED: 'success', FULL_AWARD: 'success', CLEAR: 'success', COMPLETED: 'success',
  DISBURSED: 'success', FUND_AT_FULL_AMOUNT: 'success',
  // in-progress / info
  SUBMITTED: 'info', UNDER_REVIEW: 'info', ACCEPTED_ASSIGN: 'info', IN_PREPARATION: 'info',
  EVIDENCE_SUBMITTED: 'info', ASSIGNED: 'info', INVITED: 'info', FILED: 'info',
  REDUCED_AWARD: 'info', FUND_AT_REDUCED: 'info', RESERVE_LIST: 'info', UNREAD: 'info',
  // pending / warning
  DRAFT: 'warning', UPCOMING: 'warning', PENDING: 'warning', SUSPENDED: 'warning',
  REVISION_REQUESTED: 'warning', OVERDUE: 'danger', COI_DECLARED: 'warning',
  // negative / danger
  DECLINED: 'danger', WITHDRAWN: 'danger', REJECTED: 'danger', FAILED: 'danger',
  DO_NOT_FUND: 'danger', TERMINATED: 'danger', ABANDONED: 'danger', INACTIVE: 'neutral',
  // neutral
  CLOSED: 'neutral', READ: 'neutral', DISMISSED: 'neutral',
};

/** Pretty-print an UPPER_SNAKE_CASE enum constant, e.g. UNDER_REVIEW -> "Under Review". */
export function humanize(value: string | null | undefined): string {
  if (!value) return '';
  return value
    .toLowerCase()
    .split('_')
    .map((w) => w.charAt(0).toUpperCase() + w.slice(1))
    .join(' ');
}
