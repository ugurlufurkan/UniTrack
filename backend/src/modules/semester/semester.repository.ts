import { and, eq } from "drizzle-orm";

import { db } from "../../db";
import { semesters } from "../../db/schema";

export const findAll = async (userId: string) => {
  return db
    .select()
    .from(semesters)
    .where(eq(semesters.userId, userId));
};

export const findById = async (
  userId: string,
  semesterId: string
) => {
  const result = await db
    .select()
    .from(semesters)
    .where(
      and(
        eq(semesters.id, semesterId),
        eq(semesters.userId, userId)
      )
    )
    .limit(1);

  return result[0];
};

export const create = async (
  userId: string,
  year: number,
  term: string
) => {
  const [semester] = await db
    .insert(semesters)
    .values({
      userId,
      year,
      term,
    })
    .returning();

  return semester;
};

export const update = async (
  userId: string,
  semesterId: string,
  year: number,
  term: string
) => {
  const [semester] = await db
    .update(semesters)
    .set({
      year,
      term,
    })
    .where(
      and(
        eq(semesters.id, semesterId),
        eq(semesters.userId, userId)
      )
    )
    .returning();

  return semester;
};

export const remove = async (
  userId: string,
  semesterId: string
) => {
  await db
    .delete(semesters)
    .where(
      and(
        eq(semesters.id, semesterId),
        eq(semesters.userId, userId)
      )
    );
};

export const exists = async (
  userId: string,
  year: number,
  term: string
) => {
  const result = await db
    .select()
    .from(semesters)
    .where(
      and(
        eq(semesters.userId, userId),
        eq(semesters.year, year),
        eq(semesters.term, term)
      )
    )
    .limit(1);

  return !!result.length;
};