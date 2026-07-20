import { eq } from "drizzle-orm";

import { db } from "../../db";
import { users } from "../../db/schema";
import { AppError } from "../../shared/errors/AppError";
import { SettingsUpdateInput } from "../../shared/validation/settings.schema";

const SETTINGS_COLUMNS = {
  themePreference: users.themePreference,
  targetGpa: users.targetGpa,
  examPeriodStart: users.examPeriodStart,
  examPeriodEnd: users.examPeriodEnd,
} as const;

export const getSettings = async (userId: string) => {
  const [row] = await db
    .select(SETTINGS_COLUMNS)
    .from(users)
    .where(eq(users.id, userId))
    .limit(1);

  if (!row) {
    throw AppError.notFound("Kullanıcı bulunamadı.");
  }

  return row;
};

export const updateSettings = async (
  userId: string,
  data: SettingsUpdateInput
) => {
  // ÖNEMLİ: `"alan" in data` KULLANMA — zod, optional() alanları client hiç
  // göndermese bile parse sonucunda `undefined` değerle birlikte objede
  // tutabiliyor, yani `in` kontrolü her zaman true dönüp gönderilmeyen
  // alanları da (yanlışlıkla) null'a sıfırlayabilir. Bunun yerine değerin
  // gerçekten `undefined` olup olmadığına bakıyoruz: undefined = client bu
  // alana hiç dokunmadı, null = client bilerek sıfırladı.
  const patch: Partial<typeof users.$inferInsert> = {};

  if (data.themePreference !== undefined) patch.themePreference = data.themePreference;
  if (data.targetGpa !== undefined) patch.targetGpa = data.targetGpa;
  if (data.examPeriodStart !== undefined) patch.examPeriodStart = data.examPeriodStart;
  if (data.examPeriodEnd !== undefined) patch.examPeriodEnd = data.examPeriodEnd;

  if (Object.keys(patch).length === 0) {
    return getSettings(userId);
  }

  const [row] = await db
    .update(users)
    .set(patch)
    .where(eq(users.id, userId))
    .returning(SETTINGS_COLUMNS);

  if (!row) {
    throw AppError.notFound("Kullanıcı bulunamadı.");
  }

  return row;
};
