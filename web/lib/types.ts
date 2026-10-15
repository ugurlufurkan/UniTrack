export type User = { userId: string; email: string; name?: string; isEmailVerified?: boolean };
export type Semester = { id: string; year: number; term: string };
export type GradeBand = { letter: string; min: number; point: number };
export type Component = { id?: string; name: string; weight: number; score: number | null };
export type Course = {
  id: string;
  semesterId: string;
  name: string;
  credit: number;
  average: number | null;
  letterGrade: string | null;
  gradePoint: number | null;
  passed: boolean | null;
  gradeScale?: GradeBand[] | null;
  components: Component[];
};
export type Checklist = { id: string; title: string; isDone: boolean };
export type Event = {
  id: string;
  title: string;
  description: string | null;
  type: string;
  startAt: string;
  endAt: string | null;
  courseId: string | null;
  courseName?: string | null;
  location: string | null;
  status: string;
  priority: string;
  checklist?: Checklist[];
};
export type Schedule = {
  id: string;
  courseId: string;
  courseName: string;
  dayOfWeek: number;
  startTime: string;
  endTime: string;
  location: string | null;
};
export type Attendance = {
  courseId: string;
  courseName: string;
  totalWeeks: number;
  weeklyHours: number;
  attendedHours: number;
  absentHours: number;
  excusedHours: number;
  attendanceLimitHours: number;
  remainingAllowedHours: number | null;
  absenceRatePercent: number;
  isAtRisk: boolean;
  records?: AttendanceRecord[];
};
export type AttendanceRecord = {
  id: string;
  weekNumber: number;
  date: string;
  attendedHours: number;
  absentHours: number;
  excusedHours: number;
  note: string | null;
};
export type Settings = {
  themePreference: 'SYSTEM' | 'LIGHT' | 'DARK' | null;
  targetGpa: number | null;
  examPeriodStart: string | null;
  examPeriodEnd: string | null;
};
export type Dashboard = {
  totalSemesters: number;
  totalCourses: number;
  totalCredits: number;
  passedCourses: number;
  failedCourses: number;
  ongoingCourses: number;
  gpa: number;
  weeklyLessonCount: number;
  attendanceOverview: { atRiskCourses: number } | null;
};
export type Statistics = {
  totalCourses: number;
  totalCredits: number;
  overallAverage: number;
  passedCourses: number;
  failedCourses: number;
  ongoingCourses: number;
  semesterGpa: { semesterId: string; semester: string; gpa: number }[];
};
