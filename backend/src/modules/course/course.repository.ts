import { and, asc, eq, inArray } from "drizzle-orm";

import { db } from "../../db";
import { courses, courseComponents, semesters, users } from "../../db/schema";
import { CreateCourseDto, UpdateCourseDto } from "./course.types";

import { calculateGrade, resolveGradeScale } from "../../shared/utils/grade.util";
import { AppError } from "../../shared/errors/AppError";

const getUserDefaultScale = async (userId: string) => {
  const [user] = await db
    .select({ defaultGradeScale: users.defaultGradeScale })
    .from(users)
    .where(eq(users.id, userId))
    .limit(1);

  return user?.defaultGradeScale ?? null;
};

const attachComponents = async (courseRows: (typeof courses.$inferSelect)[]) => {
  if (!courseRows.length) return [];

  const ids = courseRows.map((c) => c.id);

  const components = await db
    .select()
    .from(courseComponents)
    .where(inArray(courseComponents.courseId, ids))
    .orderBy(asc(courseComponents.sortOrder));

  const byCourse = new Map<string, typeof components>();
  for (const comp of components) {
    const list = byCourse.get(comp.courseId) ?? [];
    list.push(comp);
    byCourse.set(comp.courseId, list);
  }

  return courseRows.map((course) => ({
    ...course,
    components: byCourse.get(course.id) ?? [],
  }));
};

export const findAll = async (userId: string) => {
  const rows = await db
    .select({
      id: courses.id,
      semesterId: courses.semesterId,
      name: courses.name,
      credit: courses.credit,
      gradeScale: courses.gradeScale,
      average: courses.average,
      letterGrade: courses.letterGrade,
      gradePoint: courses.gradePoint,
      passed: courses.passed,
      createdAt: courses.createdAt,
    })
    .from(courses)
    .innerJoin(semesters, eq(courses.semesterId, semesters.id))
    .where(eq(semesters.userId, userId));

  return attachComponents(rows as any);
};

export const findOne = async (userId: string, courseId: string) => {
  const rows = await db
    .select()
    .from(courses)
    .innerJoin(semesters, eq(courses.semesterId, semesters.id))
    .where(and(eq(courses.id, courseId), eq(semesters.userId, userId)))
    .limit(1);

  if (!rows.length) {
    throw AppError.notFound("Ders bulunamadı.");
  }

  return rows[0].courses;
};

export const create = async (userId: string, data: CreateCourseDto) => {
  const semester = await db
    .select()
    .from(semesters)
    .where(and(eq(semesters.id, data.semesterId), eq(semesters.userId, userId)))
    .limit(1);

  if (!semester.length) {
    throw AppError.badRequest("Dönem bulunamadı.");
  }

  const userDefaultScale = await getUserDefaultScale(userId);
  const scale = resolveGradeScale(data.gradeScale, userDefaultScale);

  const grade = calculateGrade(
    data.components.map((c) => ({ weight: c.weight, score: c.score ?? null })),
    scale
  );

  return db.transaction(async (tx) => {
    const [course] = await tx
      .insert(courses)
      .values({
        semesterId: data.semesterId,
        name: data.name,
        credit: data.credit,
        gradeScale: data.gradeScale ?? null,
        average: grade.average,
        letterGrade: grade.letterGrade,
        gradePoint: grade.gradePoint,
        passed: grade.passed,
      })
      .returning();

    const insertedComponents = await tx
      .insert(courseComponents)
      .values(
        data.components.map((c, index) => ({
          courseId: course.id,
          name: c.name,
          weight: c.weight,
          score: c.score ?? null,
          sortOrder: index,
        }))
      )
      .returning();

    return { ...course, components: insertedComponents };
  });
};

export const update = async (
  userId: string,
  courseId: string,
  data: UpdateCourseDto
) => {
  const existing = await db
    .select()
    .from(courses)
    .innerJoin(semesters, eq(courses.semesterId, semesters.id))
    .where(and(eq(courses.id, courseId), eq(semesters.userId, userId)))
    .limit(1);

  if (!existing.length) {
    throw AppError.notFound("Ders bulunamadı.");
  }

  const current = existing[0].courses;

  const currentComponents = await db
    .select()
    .from(courseComponents)
    .where(eq(courseComponents.courseId, courseId))
    .orderBy(asc(courseComponents.sortOrder));

  const effectiveComponents = data.components ?? currentComponents;
  const effectiveGradeScale =
    data.gradeScale !== undefined ? data.gradeScale : current.gradeScale;

  const userDefaultScale = await getUserDefaultScale(userId);
  const scale = resolveGradeScale(effectiveGradeScale, userDefaultScale);

  const grade = calculateGrade(
    effectiveComponents.map((c) => ({ weight: c.weight, score: c.score ?? null })),
    scale
  );

  return db.transaction(async (tx) => {
    const [updated] = await tx
      .update(courses)
      .set({
        name: data.name ?? current.name,
        credit: data.credit ?? current.credit,
        gradeScale: effectiveGradeScale ?? null,
        average: grade.average,
        letterGrade: grade.letterGrade,
        gradePoint: grade.gradePoint,
        passed: grade.passed,
      })
      .where(eq(courses.id, courseId))
      .returning();

    let components = currentComponents;

    if (data.components) {
      await tx
        .delete(courseComponents)
        .where(eq(courseComponents.courseId, courseId));

      components = await tx
        .insert(courseComponents)
        .values(
          data.components.map((c, index) => ({
            courseId,
            name: c.name,
            weight: c.weight,
            score: c.score ?? null,
            sortOrder: index,
          }))
        )
        .returning();
    }

    return { ...updated, components };
  });
};

export const remove = async (userId: string, courseId: string) => {
  const course = await db
    .select()
    .from(courses)
    .innerJoin(semesters, eq(courses.semesterId, semesters.id))
    .where(and(eq(courses.id, courseId), eq(semesters.userId, userId)))
    .limit(1);

  if (!course.length) {
    throw AppError.notFound("Ders bulunamadı.");
  }

  await db.delete(courses).where(eq(courses.id, courseId));
};
