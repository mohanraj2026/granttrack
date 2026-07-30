import { CallStatus, ReviewMethod, SchemeStatus } from './enums';

export interface SponsorRequest {
  name: string;
  type: string;
  contactEmail: string;
  phone: string;
  address: string;
  website: string;
}
export interface SponsorResponse {
  id: number;
  sponsorCode?: string;
  name: string;
  type?: string;
  contactEmail?: string;
  phone?: string;
  address?: string;
  website?: string;
}

export interface InstitutionRequest {
  name: string;
  type: string;
  country: string;
  universityName: string;
  address: string;
  city: string;
  state: string;
  pincode: string;
  mobileNumber?: string;
  email?: string;
}
export interface InstitutionResponse {
  id: number;
  institutionCode?: string;
  name: string;
  type?: string;
  country?: string;
  universityName?: string;
  address?: string;
  city?: string;
  state?: string;
  pincode?: string;
  mobileNumber?: string;
  email?: string;
}

export interface FundingSchemeRequest {
  schemeName: string;
  sponsorId: number;
  researchArea: string;
  category: string;
  maxAwardAmount: number;
  minAwardAmount: number;
  eligibleApplicants: string;
  fundingDurationMonths?: number;
  fromDate?: string;
  toDate?: string;
  description?: string;
  status?: SchemeStatus;
}
export interface FundingSchemeResponse {
  id: number;
  schemeCode?: string;
  schemeName: string;
  sponsorId: number;
  sponsorName?: string;
  researchArea?: string;
  category?: string;
  maxAwardAmount: number;
  minAwardAmount: number;
  eligibleApplicants?: string;
  fundingDurationMonths?: number;
  fromDate?: string;
  toDate?: string;
  description?: string;
  documentPath?: string;
  status: SchemeStatus;
  createdAt?: string;
  updatedAt?: string;
}

export interface GrantCallRequest {
  schemeId: number;
  callTitle: string;
  openDate: string;
  closeDate: string;
  expectedAwards?: number;
  totalBudgetAllocated?: number;
  reviewMethod: ReviewMethod;
}
export interface GrantCallResponse {
  id: number;
  schemeId: number;
  schemeName?: string;
  schemeCategory?: string;
  eligibleApplicants?: string;
  fundingDurationMonths?: number;
  schemeDocumentPath?: string;
  schemeMaxAwardAmount?: number;
  callTitle: string;
  openDate: string;
  closeDate: string;
  expectedAwards?: number;
  totalBudgetAllocated?: number;
  reviewMethod: ReviewMethod;
  status: CallStatus;
  createdAt?: string;
  updatedAt?: string;
}
