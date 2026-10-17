import { and, eq, ne } from "drizzle-orm";

import { db } from "../../db";
import { semesters, users } from "../../db/schema";
import { AppError } from "../../shared/errors/AppError";

export const findAll = async (userId: string) => {
  return db.select().from(semesters).where(eq(semesters.userId, userId));
};

export const findById = async (userId: string, semesterId: string) => {
  const result = await db
    .select()
    .from(semesters)
    .where(and(eq(semesters.id, semesterId), eq(semesters.userId, userId)))
    .limit(1);

  return result[0];
};

export const create = async (userId: string, year: number, term: string) => {
  return db.transaction(async (tx) => {
    const [owner] = await tx
      .select({ id: users.id })
      .from(users)
      .where(eq(users.id, userId))
      .for("no key update");
    if (!owner) throw AppError.notFound("Kullanıcı bulunamadı.");
    const [duplicate] = await tx
      .select({ id: semesters.id })
      .from(semesters)
      .where(
        and(
          eq(semesters.userId, userId),
          eq(semesters.year, year),
          eq(semesters.term, term),
        ),
      );
    if (duplicate) throw AppError.conflict("Bu dönem zaten mevcut.");
    const [semester] = await tx
      .insert(semesters)
      .values({
        userId,
        year,
        term,
      })
      .returning();

    return semester;
  });
};

export const update = async (
  userId: string,
  semesterId: string,
  year: number,
  term: string,
) => {
  return db.transaction(async (tx) => {
    await tx
      .select({ id: users.id })
      .from(users)
      .where(eq(users.id, userId))
      .for("no key update");
    const [duplicate] = await tx
      .select({ id: semesters.id })
      .from(semesters)
      .where(
        and(
          eq(semesters.userId, userId),
          eq(semesters.year, year),
          eq(semesters.term, term),
          ne(semesters.id, semesterId),
        ),
      );
    if (duplicate) throw AppError.conflict("Bu dönem zaten mevcut.");
    const [semester] = await tx
      .update(semesters)
      .set({
        year,
        term,
      })
      .where(and(eq(semesters.id, semesterId), eq(semesters.userId, userId)))
      .returning();

    if (!semester) throw AppError.notFound("Dönem bulunamadı.");
    return semester;
  });
};

export const remove = async (userId: string, semesterId: string) => {
  await db
    .delete(semesters)
    .where(and(eq(semesters.id, semesterId), eq(semesters.userId, userId)));
};

export const exists = async (userId: string, year: number, term: string) => {
  const result = await db
    .select()
    .from(semesters)
    .where(
      and(
        eq(semesters.userId, userId),
        eq(semesters.year, year),
        eq(semesters.term, term),
      ),
    )
    .limit(1);

  return !!result.length;
};
