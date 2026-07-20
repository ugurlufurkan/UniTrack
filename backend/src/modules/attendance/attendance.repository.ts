import { and, asc, eq, inArray } from "drizzle-orm";

import { db } from "../../db";
import { attendanceRecords, courses, semesters } from "../../db/schema";
import { AppError } from "../../shared/errors/AppError";
import {
  UpdateAttendanceRecordDto,
  UpdateAttendanceSettingsDto,
  UpsertAttendanceRecordDto,
} from "./attendance.types";

type CourseAttendanceContext = {
  id: string;
  name: string;
  totalWeeks: number;
  weeklyHours: number;
  attendanceLimitHours: number;
};

const verifyCourseOwnership = async (
  userId: string,
  courseId: string
): Promise<CourseAttendanceContext> => {
  const rows = await db
    .select({
      id: courses.id,
      name: courses.name,
      totalWeeks: courses.totalWeeks,
      weeklyHours: courses.weeklyHours,
      attendanceLimitHours: courses.attendanceLimitHours,
    })
    .from(courses)
    .innerJoin(semesters, eq(courses.semesterId, semesters.id))
    .where(and(eq(courses.id, courseId), eq(semesters.userId, userId)))
    .limit(1);

  if (!rows.length) {
    throw AppError.notFound("Ders bulunamadı.");
  }

  return rows[0];
};

const buildSummary = (
  course: CourseAttendanceContext,
  records: (typeof attendanceRecords.$inferSelect)[]
) => {
  const totalCourseHours = course.totalWeeks * course.weeklyHours;

  const attendedHours = records.reduce((sum, r) => sum + r.attendedHours, 0);
  const absentHours = records.reduce((sum, r) => sum + r.absentHours, 0);
  const excusedHours = records.reduce((sum, r) => sum + r.excusedHours, 0);

  const markedWeeks = records.length;
  const unmarkedWeeks = Math.max(course.totalWeeks - markedWeeks, 0);

  const absenceRatePercent =
    totalCourseHours > 0 ? (absentHours / totalCourseHours) * 100 : 0;

  const hasLimit = course.attendanceLimitHours > 0;
  const remainingAllowedHours = hasLimit
    ? Math.max(course.attendanceLimitHours - absentHours, 0)
    : null;
  const isAtRisk = hasLimit && absentHours >= course.attendanceLimitHours;

  return {
    courseId: course.id,
    courseName: course.name,
    totalWeeks: course.totalWeeks,
    weeklyHours: course.weeklyHours,
    totalCourseHours,
    attendedHours,
    absentHours,
    excusedHours,
    unmarkedWeeks,
    absenceRatePercent: Math.round(absenceRatePercent * 100) / 100,
    attendanceLimitHours: course.attendanceLimitHours,
    remainingAllowedHours,
    isAtRisk,
  };
};

export const findAllCoursesWithSummary = async (userId: string) => {
  const courseRows = await db
    .select({
      id: courses.id,
      name: courses.name,
      totalWeeks: courses.totalWeeks,
      weeklyHours: courses.weeklyHours,
      attendanceLimitHours: courses.attendanceLimitHours,
    })
    .from(courses)
    .innerJoin(semesters, eq(courses.semesterId, semesters.id))
    .where(eq(semesters.userId, userId));

  if (!courseRows.length) return [];

  const courseIds = courseRows.map((c) => c.id);

  const records = await db
    .select()
    .from(attendanceRecords)
    .where(inArray(attendanceRecords.courseId, courseIds));

  const byCourse = new Map<string, (typeof attendanceRecords.$inferSelect)[]>();
  for (const record of records) {
    const list = byCourse.get(record.courseId) ?? [];
    list.push(record);
    byCourse.set(record.courseId, list);
  }

  return courseRows.map((course) =>
    buildSummary(course, byCourse.get(course.id) ?? [])
  );
};

export const findCourseAttendance = async (userId: string, courseId: string) => {
  const course = await verifyCourseOwnership(userId, courseId);

  const records = await db
    .select()
    .from(attendanceRecords)
    .where(eq(attendanceRecords.courseId, courseId))
    .orderBy(asc(attendanceRecords.weekNumber));

  return {
    ...buildSummary(course, records),
    records,
  };
};

export const getOverview = async (userId: string) => {
  const summaries = await findAllCoursesWithSummary(userId);

  const totalCourses = summaries.length;
  const atRiskCourses = summaries.filter((s) => s.isAtRisk).length;

  const averageAbsenceRate =
    totalCourses > 0
      ? Math.round(
          (summaries.reduce((sum, s) => sum + s.absenceRatePercent, 0) /
            totalCourses) *
            100
        ) / 100
      : 0;

  return {
    totalCourses,
    atRiskCourses,
    averageAbsenceRate,
    courses: summaries,
  };
};

const assertHoursWithinWeeklyLimit = (
  weeklyHours: number,
  attendedHours: number,
  absentHours: number,
  excusedHours: number
) => {
  const total = attendedHours + absentHours + excusedHours;
  if (total > weeklyHours) {
    throw AppError.badRequest(
      `Bu ders haftada ${weeklyHours} saat — girilen saatlerin toplamı (${total}) bunu geçemez.`
    );
  }
};

export const upsertRecord = async (
  userId: string,
  courseId: string,
  data: UpsertAttendanceRecordDto
) => {
  const course = await verifyCourseOwnership(userId, courseId);

  if (data.weekNumber > course.totalWeeks) {
    throw AppError.badRequest(
      `Bu ders ${course.totalWeeks} haftalık — hafta numarası bu değeri geçemez.`
    );
  }

  assertHoursWithinWeeklyLimit(
    course.weeklyHours,
    data.attendedHours,
    data.absentHours,
    data.excusedHours
  );

  const existing = await db
    .select()
    .from(attendanceRecords)
    .where(
      and(
        eq(attendanceRecords.courseId, courseId),
        eq(attendanceRecords.weekNumber, data.weekNumber)
      )
    )
    .limit(1);

  if (existing.length) {
    const [updated] = await db
      .update(attendanceRecords)
      .set({
        date: new Date(data.date),
        attendedHours: data.attendedHours,
        absentHours: data.absentHours,
        excusedHours: data.excusedHours,
        note: data.note ?? null,
        updatedAt: new Date(),
      })
      .where(eq(attendanceRecords.id, existing[0].id))
      .returning();

    return updated;
  }

  const [created] = await db
    .insert(attendanceRecords)
    .values({
      userId,
      courseId,
      weekNumber: data.weekNumber,
      date: new Date(data.date),
      attendedHours: data.attendedHours,
      absentHours: data.absentHours,
      excusedHours: data.excusedHours,
      note: data.note ?? null,
    })
    .returning();

  return created;
};

export const updateRecord = async (
  userId: string,
  recordId: string,
  data: UpdateAttendanceRecordDto
) => {
  const existing = await db
    .select()
    .from(attendanceRecords)
    .where(
      and(eq(attendanceRecords.id, recordId), eq(attendanceRecords.userId, userId))
    )
    .limit(1);

  if (!existing.length) {
    throw AppError.notFound("Devamsızlık kaydı bulunamadı.");
  }

  const current = existing[0];

  const nextAttended = data.attendedHours ?? current.attendedHours;
  const nextAbsent = data.absentHours ?? current.absentHours;
  const nextExcused = data.excusedHours ?? current.excusedHours;

  const [courseRow] = await db
    .select({ weeklyHours: courses.weeklyHours })
    .from(courses)
    .where(eq(courses.id, current.courseId))
    .limit(1);

  assertHoursWithinWeeklyLimit(
    courseRow?.weeklyHours ?? 24,
    nextAttended,
    nextAbsent,
    nextExcused
  );

  const [updated] = await db
    .update(attendanceRecords)
    .set({
      date: data.date ? new Date(data.date) : current.date,
      attendedHours: nextAttended,
      absentHours: nextAbsent,
      excusedHours: nextExcused,
      note: data.note !== undefined ? data.note : current.note,
      updatedAt: new Date(),
    })
    .where(eq(attendanceRecords.id, recordId))
    .returning();

  return updated;
};

export const removeRecord = async (userId: string, recordId: string) => {
  const existing = await db
    .select()
    .from(attendanceRecords)
    .where(
      and(eq(attendanceRecords.id, recordId), eq(attendanceRecords.userId, userId))
    )
    .limit(1);

  if (!existing.length) {
    throw AppError.notFound("Devamsızlık kaydı bulunamadı.");
  }

  await db.delete(attendanceRecords).where(eq(attendanceRecords.id, recordId));
};

export const updateCourseSettings = async (
  userId: string,
  courseId: string,
  data: UpdateAttendanceSettingsDto
) => {
  await verifyCourseOwnership(userId, courseId);

  const patch: Partial<typeof courses.$inferInsert> = {};
  if (data.totalWeeks !== undefined) patch.totalWeeks = data.totalWeeks;
  if (data.weeklyHours !== undefined) patch.weeklyHours = data.weeklyHours;
  if (data.attendanceLimitHours !== undefined) {
    patch.attendanceLimitHours = data.attendanceLimitHours;
  }

  const [updated] = await db
    .update(courses)
    .set(patch)
    .where(eq(courses.id, courseId))
    .returning();

  return updated;
};
