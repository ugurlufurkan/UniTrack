import { z } from "zod";

const hoursField = z.number().int().min(0).max(24);

export const upsertAttendanceRecordSchema = z.object({
  weekNumber: z.number().int().min(1).max(52),
  date: z.string().min(1, "Tarih zorunludur."),
  attendedHours: hoursField.default(0),
  absentHours: hoursField.default(0),
  excusedHours: hoursField.default(0),
  note: z.string().max(500).nullable().optional(),
});

export const updateAttendanceRecordSchema = z.object({
  date: z.string().min(1).optional(),
  attendedHours: hoursField.optional(),
  absentHours: hoursField.optional(),
  excusedHours: hoursField.optional(),
  note: z.string().max(500).nullable().optional(),
});

export const updateAttendanceSettingsSchema = z
  .object({
    totalWeeks: z.number().int().min(1).max(52).optional(),
    weeklyHours: z.number().int().min(1).max(24).optional(),
    attendanceLimitHours: z.number().int().min(0).max(500).optional(),
  })
  .refine(
    (data) =>
      data.totalWeeks !== undefined ||
      data.weeklyHours !== undefined ||
      data.attendanceLimitHours !== undefined,
    {
      message:
        "totalWeeks, weeklyHours veya attendanceLimitHours alanlarından en az biri gönderilmelidir.",
    }
  );
