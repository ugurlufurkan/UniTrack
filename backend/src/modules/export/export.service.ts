import { eq } from "drizzle-orm";

import { db } from "../../db";
import { attendanceRecords, courseSchedule, users } from "../../db/schema";
import { AppError } from "../../shared/errors/AppError";

import * as semesterRepository from "../semester/semester.repository";
import * as courseRepository from "../course/course.repository";
import * as calendarRepository from "../calendar/calendar.repository";
import * as settingsRepository from "../settings/settings.repository";

const EXPORT_FORMAT_VERSION = 1;

/**
 * Kullanıcının UniTrack'teki TÜM verisini tek bir JSON objesinde toplar.
 * Kullanım alanları:
 *  - "Verilerimi dışa aktar" (Ayarlar > Hesabım) — kullanıcı elindeki bir
 *    kopyayı indirip saklayabilir (KVKK/GDPR "verilerimin taşınabilirliği").
 *  - Destek/hata ayıklama sırasında kullanıcının durumunu incelemek.
 *
 * Not: Bu, hesap SİLİNDİĞİNDE tutulan bir yedek değil — hesap silme ayrı bir
 * akış. Bu sadece "anlık görüntü" (snapshot) indirme.
 */
export const exportAllUserData = async (userId: string) => {
  const [profile] = await db
    .select({
      id: users.id,
      email: users.email,
      name: users.name,
      picture: users.picture,
      defaultGradeScale: users.defaultGradeScale,
      createdAt: users.createdAt,
    })
    .from(users)
    .where(eq(users.id, userId))
    .limit(1);

  if (!profile) {
    throw AppError.notFound("Kullanıcı bulunamadı.");
  }

  const [semesters, courses, events, schedule, settings, attendance] =
    await Promise.all([
      semesterRepository.findAll(userId),
      courseRepository.findAll(userId),
      calendarRepository.findAllEvents(userId, {}),
      calendarRepository.findAllSchedule(userId),
      settingsRepository.getSettings(userId),
      db
        .select()
        .from(attendanceRecords)
        .where(eq(attendanceRecords.userId, userId)),
    ]);

  return {
    formatVersion: EXPORT_FORMAT_VERSION,
    exportedAt: new Date().toISOString(),
    profile,
    settings,
    semesters,
    courses,
    events,
    courseSchedule: schedule,
    attendanceRecords: attendance,
  };
};
