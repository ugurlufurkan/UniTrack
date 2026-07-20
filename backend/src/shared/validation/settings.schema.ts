import { z } from "zod";

// Hepsi opsiyonel: istemci sadece değişen alanı gönderir (PATCH semantiği).
// null gönderilirse o ayar sıfırlanır (varsayılana döner).
export const settingsUpdateSchema = z
  .object({
    themePreference: z.enum(["SYSTEM", "LIGHT", "DARK"]).nullable().optional(),

    targetGpa: z.number().min(0).max(4.5).nullable().optional(),

    examPeriodStart: z.coerce.date().nullable().optional(),
    examPeriodEnd: z.coerce.date().nullable().optional(),
  })
  .refine(
    (data) =>
      !(data.examPeriodStart && data.examPeriodEnd) ||
      data.examPeriodStart <= data.examPeriodEnd,
    {
      message: "Sınav dönemi başlangıcı bitişten sonra olamaz.",
      path: ["examPeriodStart"],
    }
  );

export type SettingsUpdateInput = z.infer<typeof settingsUpdateSchema>;
