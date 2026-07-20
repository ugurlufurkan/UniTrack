import { and, asc, eq, inArray } from "drizzle-orm";

import { db } from "../../db";
import { courses, semesters, taskChecklistItems, tasks } from "../../db/schema";
import { AppError } from "../../shared/errors/AppError";
import {
  CreateChecklistItemDto,
  CreateTaskDto,
  TaskDto,
  TaskFilters,
  UpdateChecklistItemDto,
  UpdateTaskDto,
} from "./tasks.types";

type TaskRow = typeof tasks.$inferSelect & { courseName: string | null };

// Görev doğrudan userId taşıyor (attendance/calendar'daki gibi dolaylı
// semester join'ine gerek yok); sadece dersle ilişkilendirilirken dersin
// bu kullanıcıya ait olduğunu doğruluyoruz.
const verifyCourseOwnership = async (userId: string, courseId: string) => {
  const rows = await db
    .select({ id: courses.id })
    .from(courses)
    .innerJoin(semesters, eq(courses.semesterId, semesters.id))
    .where(and(eq(courses.id, courseId), eq(semesters.userId, userId)))
    .limit(1);

  if (!rows.length) {
    throw AppError.badRequest("Ders bulunamadı.");
  }
};

const getOwnedTask = async (userId: string, taskId: string) => {
  const rows = await db
    .select()
    .from(tasks)
    .where(and(eq(tasks.id, taskId), eq(tasks.userId, userId)))
    .limit(1);

  if (!rows.length) {
    throw AppError.notFound("Görev bulunamadı.");
  }

  return rows[0]!;
};

const getOwnedChecklistItem = async (
  userId: string,
  taskId: string,
  itemId: string
) => {
  await getOwnedTask(userId, taskId);

  const rows = await db
    .select()
    .from(taskChecklistItems)
    .where(
      and(eq(taskChecklistItems.id, itemId), eq(taskChecklistItems.taskId, taskId))
    )
    .limit(1);

  if (!rows.length) {
    throw AppError.notFound("Checklist öğesi bulunamadı.");
  }

  return rows[0]!;
};

const toDto = (
  task: typeof tasks.$inferSelect,
  courseName: string | null,
  checklist: (typeof taskChecklistItems.$inferSelect)[]
): TaskDto => {
  const sorted = [...checklist].sort((a, b) => a.sortOrder - b.sortOrder);

  return {
    id: task.id,
    courseId: task.courseId,
    courseName,
    title: task.title,
    description: task.description,
    type: task.type as TaskDto["type"],
    dueAt: task.dueAt,
    priority: task.priority as TaskDto["priority"],
    status: task.status as TaskDto["status"],
    completedAt: task.completedAt,
    checklist: sorted.map((c) => ({
      id: c.id,
      title: c.title,
      isDone: c.isDone,
      sortOrder: c.sortOrder,
    })),
    checklistTotal: sorted.length,
    checklistDone: sorted.filter((c) => c.isDone).length,
    createdAt: task.createdAt,
    updatedAt: task.updatedAt,
  };
};

const attachChecklists = async (rows: TaskRow[]): Promise<TaskDto[]> => {
  if (!rows.length) return [];

  const ids = rows.map((r) => r.id);

  const items = await db
    .select()
    .from(taskChecklistItems)
    .where(inArray(taskChecklistItems.taskId, ids))
    .orderBy(asc(taskChecklistItems.sortOrder));

  const byTask = new Map<string, (typeof taskChecklistItems.$inferSelect)[]>();
  for (const item of items) {
    const list = byTask.get(item.taskId) ?? [];
    list.push(item);
    byTask.set(item.taskId, list);
  }

  return rows.map((r) => toDto(r, r.courseName, byTask.get(r.id) ?? []));
};

export const findAll = async (
  userId: string,
  filters?: TaskFilters
): Promise<TaskDto[]> => {
  const conditions = [eq(tasks.userId, userId)];
  if (filters?.status) conditions.push(eq(tasks.status, filters.status));
  if (filters?.type) conditions.push(eq(tasks.type, filters.type));
  if (filters?.courseId) conditions.push(eq(tasks.courseId, filters.courseId));

  const rows = await db
    .select({
      id: tasks.id,
      userId: tasks.userId,
      courseId: tasks.courseId,
      title: tasks.title,
      description: tasks.description,
      type: tasks.type,
      dueAt: tasks.dueAt,
      priority: tasks.priority,
      status: tasks.status,
      completedAt: tasks.completedAt,
      createdAt: tasks.createdAt,
      updatedAt: tasks.updatedAt,
      courseName: courses.name,
    })
    .from(tasks)
    .leftJoin(courses, eq(tasks.courseId, courses.id))
    .where(and(...conditions))
    .orderBy(asc(tasks.dueAt));

  return attachChecklists(rows as unknown as TaskRow[]);
};

export const getById = async (
  userId: string,
  taskId: string
): Promise<TaskDto> => {
  const task = await getOwnedTask(userId, taskId);

  let courseName: string | null = null;
  if (task.courseId) {
    const rows = await db
      .select({ name: courses.name })
      .from(courses)
      .where(eq(courses.id, task.courseId))
      .limit(1);
    courseName = rows[0]?.name ?? null;
  }

  const items = await db
    .select()
    .from(taskChecklistItems)
    .where(eq(taskChecklistItems.taskId, taskId))
    .orderBy(asc(taskChecklistItems.sortOrder));

  return toDto(task, courseName, items);
};

export const create = async (
  userId: string,
  data: CreateTaskDto
): Promise<TaskDto> => {
  if (data.courseId) {
    await verifyCourseOwnership(userId, data.courseId);
  }

  const [inserted] = await db
    .insert(tasks)
    .values({
      userId,
      courseId: data.courseId ?? null,
      title: data.title,
      description: data.description ?? null,
      type: data.type ?? "assignment",
      dueAt: data.dueAt ? new Date(data.dueAt) : null,
      priority: data.priority ?? "medium",
    })
    .returning();

  if (data.checklist?.length) {
    await db.insert(taskChecklistItems).values(
      data.checklist.map((item, index) => ({
        taskId: inserted!.id,
        title: item.title,
        sortOrder: index,
      }))
    );
  }

  return getById(userId, inserted!.id);
};

export const update = async (
  userId: string,
  taskId: string,
  data: UpdateTaskDto
): Promise<TaskDto> => {
  await getOwnedTask(userId, taskId);

  if (data.courseId) {
    await verifyCourseOwnership(userId, data.courseId);
  }

  const values: Partial<typeof tasks.$inferInsert> = { updatedAt: new Date() };
  if (data.courseId !== undefined) values.courseId = data.courseId;
  if (data.title !== undefined) values.title = data.title;
  if (data.description !== undefined) values.description = data.description;
  if (data.type !== undefined) values.type = data.type;
  if (data.dueAt !== undefined)
    values.dueAt = data.dueAt ? new Date(data.dueAt) : null;
  if (data.priority !== undefined) values.priority = data.priority;
  if (data.status !== undefined) {
    values.status = data.status;
    values.completedAt = data.status === "completed" ? new Date() : null;
  }

  await db.update(tasks).set(values).where(eq(tasks.id, taskId));

  return getById(userId, taskId);
};

export const remove = async (userId: string, taskId: string): Promise<void> => {
  await getOwnedTask(userId, taskId);
  await db.delete(tasks).where(eq(tasks.id, taskId));
};

// Listede tek dokunuşla "Tamamlandı" işaretlemek için hızlı yol.
export const toggleStatus = async (
  userId: string,
  taskId: string
): Promise<TaskDto> => {
  const task = await getOwnedTask(userId, taskId);
  const nextStatus = task.status === "completed" ? "pending" : "completed";

  await db
    .update(tasks)
    .set({
      status: nextStatus,
      completedAt: nextStatus === "completed" ? new Date() : null,
      updatedAt: new Date(),
    })
    .where(eq(tasks.id, taskId));

  return getById(userId, taskId);
};

export const addChecklistItem = async (
  userId: string,
  taskId: string,
  data: CreateChecklistItemDto
): Promise<TaskDto> => {
  await getOwnedTask(userId, taskId);

  const existing = await db
    .select({ sortOrder: taskChecklistItems.sortOrder })
    .from(taskChecklistItems)
    .where(eq(taskChecklistItems.taskId, taskId));

  const nextOrder = existing.length
    ? Math.max(...existing.map((e) => e.sortOrder)) + 1
    : 0;

  await db.insert(taskChecklistItems).values({
    taskId,
    title: data.title,
    sortOrder: nextOrder,
  });

  return getById(userId, taskId);
};

export const updateChecklistItem = async (
  userId: string,
  taskId: string,
  itemId: string,
  data: UpdateChecklistItemDto
): Promise<TaskDto> => {
  await getOwnedChecklistItem(userId, taskId, itemId);

  const values: Partial<typeof taskChecklistItems.$inferInsert> = {};
  if (data.title !== undefined) values.title = data.title;
  if (data.isDone !== undefined) values.isDone = data.isDone;
  if (data.sortOrder !== undefined) values.sortOrder = data.sortOrder;

  if (Object.keys(values).length) {
    await db
      .update(taskChecklistItems)
      .set(values)
      .where(eq(taskChecklistItems.id, itemId));
  }

  return getById(userId, taskId);
};

export const removeChecklistItem = async (
  userId: string,
  taskId: string,
  itemId: string
): Promise<TaskDto> => {
  await getOwnedChecklistItem(userId, taskId, itemId);

  await db.delete(taskChecklistItems).where(eq(taskChecklistItems.id, itemId));

  return getById(userId, taskId);
};
