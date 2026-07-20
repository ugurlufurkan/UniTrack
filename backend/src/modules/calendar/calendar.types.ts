export type EventType =
  | "lesson"
  | "exam"
  | "quiz"
  | "assignment"
  | "project"
  | "presentation"
  | "other";

export type EventPriority = "low" | "medium" | "high";

export type EventStatus = "pending" | "in_progress" | "completed" | "cancelled";

export type EventRecurrence = "none" | "daily" | "weekly" | "monthly";

export interface EventNotificationDto {
  id?: string;
  daysBefore: number;
  hoursBefore: number;
  minutesBefore: number;
}

export interface CreateEventDto {
  courseId?: string | null;
  title: string;
  description?: string | null;
  type: EventType;
  startAt: string;
  endAt?: string | null;
  location?: string | null;
  priority?: EventPriority;
  status?: EventStatus;
  color?: string;
  recurrence?: EventRecurrence;
  notificationsEnabled?: boolean;
  notifications?: EventNotificationDto[];
}

export interface UpdateEventDto {
  courseId?: string | null;
  title?: string;
  description?: string | null;
  type?: EventType;
  startAt?: string;
  endAt?: string | null;
  location?: string | null;
  priority?: EventPriority;
  status?: EventStatus;
  color?: string;
  recurrence?: EventRecurrence;
  notificationsEnabled?: boolean;
  notifications?: EventNotificationDto[];
}

export interface CreateScheduleDto {
  courseId: string;
  dayOfWeek: number;
  startTime: string;
  endTime: string;
  location?: string | null;
}

export interface UpdateScheduleDto {
  courseId?: string;
  dayOfWeek?: number;
  startTime?: string;
  endTime?: string;
  location?: string | null;
}
