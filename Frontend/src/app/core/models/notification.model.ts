import { NotificationCategory, NotificationStatus } from './enums';

export interface NotificationRequest {
  userId: number;
  message: string;
  category: NotificationCategory;
}
export interface NotificationResponse {
  id: number;
  userId: number;
  message: string;
  category: NotificationCategory;
  status: NotificationStatus;
  createdAt?: string;
}
