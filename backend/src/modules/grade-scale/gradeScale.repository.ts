import { and, eq, inArray, isNull } from "drizzle-orm";

import { db } from "../../db";
import { users, courses, courseComponents, semesters, GradeBand } from "../../db/schema";
import { DEFAULT_GRADE_SCALE, calculateGrade, resolveGradeScale } from "../../shared/utils/grade.util";
import { AppError } from "../../shared/errors/AppError";

export const getDefault = async (userId: string) => {
  const [user] = await db
    .select({ defaultGradeScale: users.defaultGradeScale })
    .from(users)
    .where(eq(users.id, userId))
    .limit(1);

  if (!user) {
    throw AppError.notFound("Kullanıcı bulunamadı.");
  }

  return {
    gradeScale: user.defaultGradeScale ?? DEFAULT_GRADE_SCALE,
    isCustom: user.defaultGradeScale !== null,
  };
};

export const setDefault = async (
  userId: string,
  gradeScale: GradeBand[] | null
) => {
  return db.transaction(async (tx) => {
    const [user] = await tx
      .update(users)
      .set({ defaultGradeScale: gradeScale })
      .where(eq(users.id, userId))
      .returning({ defaultGradeScale: users.defaultGradeScale });

    if (!user) {
      throw AppError.notFound("Kullanıcı bulunamadı.");
    }

    // BUG FIX: Varsayılan skala değişince, kendine özel skalası OLMAYAN
    // (gradeScale = null) tüm derslerin ortalama/harf notu/katsayısı eskisi
    // gibi kalıyordu — çünkü bu değerler ders kaydedilirken hesaplanıp
    // tabloya yazılıyor, okurken yeniden hesaplanmıyor. Bir dersi tek tek
    // açıp kaydedince (updateCourse yeniden hesapladığı için) "düzeliyor"
    // gibi görünüyordu. Burada, yeni varsayılan skalayı kullanan derslerin
    // hepsini tek seferde yeniden hesaplayıp kaydediyoruz.
    const affected = await tx
      .select({ id: courses.id })
      .from(courses)
      .innerJoin(semesters, eq(courses.semesterId, semesters.id))
      .where(and(eq(semesters.userId, userId), isNull(courses.gradeScale)));

    if (affected.length) {
      const courseIds = affected.map((c) => c.id);

      const components = await tx
        .select()
        .from(courseComponents)
        .where(inArray(courseComponents.courseId, courseIds));

      const byCourse = new Map<string, typeof components>();
      for (const comp of components) {
        const list = byCourse.get(comp.courseId) ?? [];
        list.push(comp);
        byCourse.set(comp.courseId, list);
      }

      const scale = resolveGradeScale(null, user.defaultGradeScale);

      for (const courseId of courseIds) {
        const courseComponentsList = byCourse.get(courseId) ?? [];
        const grade = calculateGrade(
          courseComponentsList.map((c) => ({ weight: c.weight, score: c.score ?? null })),
          scale
        );

        await tx
          .update(courses)
          .set({
            average: grade.average,
            letterGrade: grade.letterGrade,
            gradePoint: grade.gradePoint,
            passed: grade.passed,
          })
          .where(eq(courses.id, courseId));
      }
    }

    return {
      gradeScale: user.defaultGradeScale ?? DEFAULT_GRADE_SCALE,
      isCustom: user.defaultGradeScale !== null,
    };
  });
};