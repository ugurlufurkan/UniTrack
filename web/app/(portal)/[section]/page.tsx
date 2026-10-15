import { notFound } from 'next/navigation';
import { Courses, Semesters, GpaSimulator } from '@/components/academics';
import { WeeklySchedule, Tasks, AcademicCalendar } from '@/components/planning';
import { Attendance } from '@/components/attendance';
import { Statistics, Transcript } from '@/components/insights';
import { Settings, Help } from '@/components/settings';

const pages = {
  courses: Courses,
  semesters: Semesters,
  schedule: WeeklySchedule,
  tasks: Tasks,
  calendar: AcademicCalendar,
  attendance: Attendance,
  gpa: GpaSimulator,
  statistics: Statistics,
  transcript: Transcript,
  settings: Settings,
  help: Help,
};
export default async function Page({ params }: { params: Promise<{ section: string }> }) {
  const { section } = await params;
  const Component = pages[section as keyof typeof pages];
  if (!Component) notFound();
  return <Component />;
}
