import * as courseRepository from "../course/course.repository";
import * as semesterRepository from "../semester/semester.repository";
import { SemesterGpa, StatisticsResponse } from "./statistics.types";

export const getStatistics = async (userId: string): Promise<StatisticsResponse> => {
  const [courses, semesters] = await Promise.all([
    courseRepository.findAll(userId),
    semesterRepository.findAll(userId),
  ]);

  const totalCourses = courses.length;

  const totalCredits = courses.reduce((sum, course) => sum + course.credit, 0);

  // Sadece notu tam girilmis (devam etmeyen) dersler ortalamaya/istatistige dahil edilir.
  const graded = courses.filter((c) => c.average !== null) as (typeof courses[number] & {
    average: number;
    passed: boolean;
  })[];

  const overallAverage =
    graded.length === 0
      ? 0
      : Number(
          (graded.reduce((sum, g) => sum + g.average, 0) / graded.length).toFixed(2)
        );

  const passedCourses = graded.filter((g) => g.passed).length;
  const failedCourses = graded.length - passedCourses;
  const ongoingCourses = totalCourses - graded.length;

  // Donem bazinda GPA: her donemin agirlikli (kredi) ortalamasi, sadece
  // notu girilmis derslerden hesaplanir. Ders yuku olmayan donemler
  // (kredi toplami 0) 0 GPA olarak degil, listede hic gosterilmez.
  const semesterMap = new Map(semesters.map((s) => [s.id, s]));

  const bySemester = new Map<string, typeof graded>();
  for (const course of graded) {
    const list = bySemester.get(course.semesterId) ?? [];
    list.push(course);
    bySemester.set(course.semesterId, list);
  }

  const termLabels: Record<string, string> = {
    fall: "Güz",
    spring: "Bahar",
    summer: "Yaz",
  };

  const semesterGpa: SemesterGpa[] = semesters
    .map((semester) => {
      const semesterCourses = bySemester.get(semester.id) ?? [];
      const totalCredit = semesterCourses.reduce((sum, c) => sum + c.credit, 0);
      if (totalCredit === 0) return null;

      const totalPoint = semesterCourses.reduce(
        (sum, c) => sum + c.credit * (c.gradePoint ?? 0),
        0
      );

      const label = termLabels[semester.term.toLowerCase()] ?? semester.term;

      return {
        semesterId: semester.id,
        semester: `${label} ${semester.year}`,
        gpa: Number((totalPoint / totalCredit).toFixed(2)),
      };
    })
    .filter((entry): entry is SemesterGpa => entry !== null)
    .sort((a, b) => {
      const sa = semesterMap.get(a.semesterId)!;
      const sb = semesterMap.get(b.semesterId)!;
      return sa.year - sb.year || sa.term.localeCompare(sb.term);
    });

  return {
    totalCourses,
    totalCredits,
    overallAverage,
    passedCourses,
    failedCourses,
    ongoingCourses,
    semesterGpa,
  };
};
