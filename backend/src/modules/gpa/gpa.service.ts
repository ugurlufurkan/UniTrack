import * as courseRepository from "../course/course.repository";

export const calculate = async (userId: string) => {
  const courses = await courseRepository.findAll(userId);

  let totalCredit = 0;
  let totalPoint = 0;

  const result = courses.map((course) => {
    // Not hesabi artik tek kaynaktan (course olusturma/guncellemede
    // grade.util.ts ile) geliyor; burada sadece onbellege alinmis
    // sonuc okunuyor, tekrar hesaplanmiyor.
    if (course.average !== null) {
      totalCredit += course.credit;
      totalPoint += course.credit * (course.gradePoint ?? 0);
    }

    return {
      name: course.name,
      credit: course.credit,
      average: course.average,
      letter: course.letterGrade,
      point: course.gradePoint,
      completed: course.average !== null,
    };
  });

  return {
    courses: result,
    gpa:
      totalCredit === 0
        ? 0
        : Number((totalPoint / totalCredit).toFixed(2)),
  };
};
