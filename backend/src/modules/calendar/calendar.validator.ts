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

const filterDate = z.union([z.iso.date(), z.iso.datetime({ offset: true })]);
export const eventFiltersSchema = z.object({
  startDate: filterDate.optional(),
  endDate: filterDate.optional(),
  type: eventTypeEnum.optional(),
}).refine(value => !value.startDate || !value.endDate || new Date(value.startDate) <= new Date(value.endDate), { message: "Tarih aralığı ters olamaz.", path: ["endDate"] });
export const upcomingEventsQuerySchema = z.object({ limit: z.coerce.number().int().min(1).max(100).default(20) });

export const eventNotificationSchema = z.object({
  id: z.string().uuid().nullable().optional(),
  daysBefore: z.number().int().min(0).max(365).default(0),
  hoursBefore: z.number().int().min(0).max(23).default(0),
  minutesBefore: z.number().int().min(0).max(59).default(0),
});

export const createEventSchema = z.object({
  courseId: z.string().uuid().nullable().optional(),
  title: z.string().trim().min(1).max(200),
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
}).refine(data => !data.endAt || new Date(data.endAt) >= new Date(data.startAt), { message: "Bitiş zamanı başlangıçtan önce olamaz.", path: ["endAt"] });

export const updateEventSchema = z.object({
  courseId: z.string().uuid().nullable().optional(),
  title: z.string().trim().min(1).max(200).optional(),
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
  startTime: z.string().regex(/^([01]\d|2[0-3]):[0-5]\d$/, "Geçerli bir saat gir (HH:MM)."),
  endTime: z.string().regex(/^([01]\d|2[0-3]):[0-5]\d$/, "Geçerli bir saat gir (HH:MM)."),
  location: z.string().max(255).nullable().optional(),
}).refine(data => data.endTime > data.startTime, { message: "Bitiş saati başlangıçtan sonra olmalı.", path: ["endTime"] });

export const updateScheduleSchema = z.object({
  courseId: z.string().uuid().optional(),
  dayOfWeek: z.number().int().min(0).max(6).optional(),
  startTime: z
    .string()
    .regex(/^([01]\d|2[0-3]):[0-5]\d$/, "Geçerli bir saat gir (HH:MM).")
    .optional(),
  endTime: z
    .string()
    .regex(/^([01]\d|2[0-3]):[0-5]\d$/, "Geçerli bir saat gir (HH:MM).")
    .optional(),
  location: z.string().max(255).nullable().optional(),
});
