import { z } from "zod";

const eventTypeEnum = z.enum([
  "lesson",
  "exam",
  "quiz",
  "assignment",
  "project",
  "presentation",
  "other",
]);

const priorityEnum = z.enum(["low", "medium", "high"]);

const statusEnum = z.enum(["pending", "in_progress", "completed", "cancelled"]);

const recurrenceEnum = z.enum(["none", "daily", "weekly", "monthly"]);

export const eventNotificationSchema = z.object({
  id: z.string().uuid().optional(),
  daysBefore: z.number().int().min(0).max(365).default(0),
  hoursBefore: z.number().int().min(0).max(23).default(0),
  minutesBefore: z.number().int().min(0).max(59).default(0),
});

export const createEventSchema = z.object({
  courseId: z.string().uuid().nullable().optional(),
  title: z.string().min(1).max(200),
  description: z.string().max(2000).nullable().optional(),
  type: eventTypeEnum,
  startAt: z.string().datetime(),
  endAt: z.string().datetime().nullable().optional(),
  location: z.string().max(255).nullable().optional(),
  priority: priorityEnum.optional(),
  status: statusEnum.optional(),
  color: z.string().max(20).optional(),
  recurrence: recurrenceEnum.optional(),
  notificationsEnabled: z.boolean().optional(),
  notifications: z.array(eventNotificationSchema).optional(),
});

export const updateEventSchema = z.object({
  courseId: z.string().uuid().nullable().optional(),
  title: z.string().min(1).max(200).optional(),
  description: z.string().max(2000).nullable().optional(),
  type: eventTypeEnum.optional(),
  startAt: z.string().datetime().optional(),
  endAt: z.string().datetime().nullable().optional(),
  location: z.string().max(255).nullable().optional(),
  priority: priorityEnum.optional(),
  status: statusEnum.optional(),
  color: z.string().max(20).optional(),
  recurrence: recurrenceEnum.optional(),
  notificationsEnabled: z.boolean().optional(),
  notifications: z.array(eventNotificationSchema).optional(),
});

export const createScheduleSchema = z.object({
  courseId: z.string().uuid(),
  dayOfWeek: z.number().int().min(0).max(6),
  startTime: z.string().regex(/^\d{2}:\d{2}$/, "Saat HH:MM formatında olmalı."),
  endTime: z.string().regex(/^\d{2}:\d{2}$/, "Saat HH:MM formatında olmalı."),
  location: z.string().max(255).nullable().optional(),
});

export const updateScheduleSchema = z.object({
  courseId: z.string().uuid().optional(),
  dayOfWeek: z.number().int().min(0).max(6).optional(),
  startTime: z
    .string()
    .regex(/^\d{2}:\d{2}$/, "Saat HH:MM formatında olmalı.")
    .optional(),
  endTime: z
    .string()
    .regex(/^\d{2}:\d{2}$/, "Saat HH:MM formatında olmalı.")
    .optional(),
  location: z.string().max(255).nullable().optional(),
});
