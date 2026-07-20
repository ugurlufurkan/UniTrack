import * as courseRepository from "../course/course.repository";
import * as semesterService from "../semester/semester.service";
import * as calendarRepository from "../calendar/calendar.repository";
import * as attendanceRepository from "../attendance/attendance.repository";

export const getDashboard = async (userId: string) => {
  const semesters = await semesterService.getAll(userId);
  const courses = await courseRepository.findAll(userId);

  // Bu haftaki toplam ders sayısı (haftalık program üzerinden — bkz.
  // ScheduleEditorSheet.kt) ve devamsızlık genel görünümü: dashboard'un
  // ana istatistiklerini bloke etmemesi için best-effort okunur.
  const [schedule, attendanceOverview] = await Promise.all([
    calendarRepository.findAllSchedule(userId).catch(() => []),
    attendanceRepository.getOverview(userId).catch(() => null),
  ]);

  let totalCredits = 0;
  let totalPoints = 0;
  let passedCourses = 0;
  let failedCourses = 0;
  let ongoingCourses = 0;

  for (const course of courses) {
    // Vize/final girilmemis, devam eden dersler "kaldi" sayilmaz;
    // GANO ve gecti/kaldi sayaclarinin disinda tutulur.
    if (course.average === null) {
      ongoingCourses++;
      continue;
    }

    totalCredits += course.credit;
    totalPoints += (course.gradePoint ?? 0) * course.credit;

    if (course.passed) {
      passedCourses++;
    } else {
      failedCourses++;
    }
  }

  return {
    totalSemesters: semesters.length,
    totalCourses: courses.length,
    totalCredits,
    passedCourses,
    failedCourses,
    ongoingCourses,
    gpa:
      totalCredits === 0
        ? 0
        : Number((totalPoints / totalCredits).toFixed(2)),
    weeklyLessonCount: schedule.length,
    attendanceOverview,
  };
};
