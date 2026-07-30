import { IpStatus, IpType, OutputStatus, OutputType } from './enums';

export interface ResearchOutputRequest {
  awardId: number;
  type: OutputType;
  title: string;
  authors?: string;
  publicationVenue?: string;
  doi?: string;
  publishedDate?: string;
  openAccessCompliant?: boolean;
  status?: OutputStatus;
}
export interface ResearchOutputResponse {
  id: number;
  awardId: number;
  type: OutputType;
  title: string;
  authors?: string;
  publicationVenue?: string;
  doi?: string;
  publishedDate?: string;
  openAccessCompliant: boolean;
  status: OutputStatus;
  createdAt?: string;
  updatedAt?: string;
}

export interface IpRecordRequest {
  awardId: number;
  ipType: IpType;
  title: string;
  inventors?: string;
  filingDate?: string;
  grantDate?: string;
  ownershipPercent?: number;
  status?: IpStatus;
}
export interface IpRecordResponse {
  id: number;
  awardId: number;
  ipType: IpType;
  title: string;
  inventors?: string;
  filingDate?: string;
  grantDate?: string;
  ownershipPercent?: number;
  status: IpStatus;
  createdAt?: string;
  updatedAt?: string;
}
