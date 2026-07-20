import { and, asc, eq, inArray } from "drizzle-orm";

import { db } from "../../db";
import { courses, eventChecklistItems, events } from "../../db/schema";
import { AppError } from "../../shared/errors/AppError";
import {
  CreateChecklistItemDto,
  ReorderChecklistDto,
  TASK_TYPES,
  UpdateChecklistItemDto,
} from "./task.types";

const attachChecklist = async (
  eventRows: (typeof events.$inferSelect & { courseName?: string | null })[]
) => {
  if (!eventRows.length) return [];

  const ids = eventRows.map((e) => e.id);

  const items = await db
    .select()
    .from(eventChecklistItems)
    .where(inArray(eventChecklistItems.eventId, ids))
    .orderBy(asc(eventChecklistItems.sortOrder));

  const byEvent = new Map<string, (typeof items)>();
  for (const item of items) {
    const list = byEvent.get(item.eventId) ?? [];
    list.push(item);
    byEvent.set(item.eventId, list);
  }

  return eventRows.map((event) => {
    const checklist = byEvent.get(event.id) ?? [];
    const doneCount = checklist.filter((c) => c.isDone).length;

    return {
      ...event,
      checklist,
      checklistTotal: checklist.length,
      checklistDone: doneCount,
    };
  });
};

const verifyTaskOwnership = async (userId: string, eventId: string) => {
  const rows = await db
    .select()
    .from(events)
    .where(and(eq(events.id, eventId), eq(events.userId, userId)))
    .limit(1);

  if (!rows.length) {
    throw AppError.notFound("Görev bulunamadı.");
  }

  return rows[0];
};

const verifyChecklistItemOwnership = async (userId: string, itemId: string) => {
  const rows = await db
    .select({
      item: eventChecklistItems,
      eventUserId: events.userId,
    })
    .from(eventChecklistItems)
    .innerJoin(events, eq(eventChecklistItems.eventId, events.id))
    .where(eq(eventChecklistItems.id, itemId))
    .limit(1);

  if (!rows.length || rows[0].eventUserId !== userId) {
    throw AppError.notFound("Alt görev bulunamadı.");
  }

  return rows[0].item;
};

export const findAllTasks = async (
  userId: string,
  filters?: { status?: string; type?: string }
) => {
  const conditions = [eq(events.userId, userId), inArray(events.type, TASK_TYPES)];

  if (filters?.status) {
    conditions.push(eq(events.status, filters.status));
  }
  if (filters?.type) {
    conditions.push(eq(events.type, filters.type));
  }

  const rows = await db
    .select({
      id: events.id,
      userId: events.userId,
      courseId: events.courseId,
      courseName: courses.name,
      title: events.title,
      description: events.description,
      type: events.type,
      startAt: events.startAt,
      endAt: events.endAt,
      location: events.location,
      priority: events.priority,
      status: events.status,
      color: events.color,
      recurrence: events.recurrence,
      notificationsEnabled: events.notificationsEnabled,
      createdAt: events.createdAt,
      updatedAt: events.updatedAt,
    })
    .from(events)
    .leftJoin(courses, eq(events.courseId, courses.id))
    .where(and(...conditions))
    .orderBy(asc(events.startAt));

  return attachChecklist(rows as any);
};

export const findTaskById = async (userId: string, eventId: string) => {
  const event = await verifyTaskOwnership(userId, eventId);
  const [withChecklist] = await attachChecklist([event]);
  return withChecklist;
};

export const createChecklistItem = async (
  userId: string,
  eventId: string,
  data: CreateChecklistItemDto
) => {
  await verifyTaskOwnership(userId, eventId);

  const existing = await db
    .select({ sortOrder: eventChecklistItems.sortOrder })
    .from(eventChecklistItems)
    .where(eq(eventChecklistItems.eventId, eventId))
    .orderBy(asc(eventChecklistItems.sortOrder));

  const nextSortOrder = existing.length
    ? existing[existing.length - 1].sortOrder + 1
    : 0;

  const [item] = await db
    .insert(eventChecklistItems)
    .values({
      eventId,
      title: data.title,
      sortOrder: nextSortOrder,
    })
    .returning();

  return item;
};

export const updateChecklistItem = async (
  userId: string,
  itemId: string,
  data: UpdateChecklistItemDto
) => {
  const current = await verifyChecklistItemOwnership(userId, itemId);

  const [updated] = await db
    .update(eventChecklistItems)
    .set({
      title: data.title ?? current.title,
      isDone: data.isDone ?? current.isDone,
      sortOrder: data.sortOrder ?? current.sortOrder,
      updatedAt: new Date(),
    })
    .where(eq(eventChecklistItems.id, itemId))
    .returning();

  return updated;
};

export const removeChecklistItem = async (userId: string, itemId: string) => {
  await verifyChecklistItemOwnership(userId, itemId);
  await db.delete(eventChecklistItems).where(eq(eventChecklistItems.id, itemId));
};

export const reorderChecklist = async (
  userId: string,
  eventId: string,
  data: ReorderChecklistDto
) => {
  await verifyTaskOwnership(userId, eventId);

  const current = await db
    .select({ id: eventChecklistItems.id })
    .from(eventChecklistItems)
    .where(eq(eventChecklistItems.eventId, eventId));

  const currentIds = new Set(current.map((c) => c.id));
  const providedIds = new Set(data.itemIds);

  if (
    currentIds.size !== providedIds.size ||
    [...currentIds].some((id) => !providedIds.has(id))
  ) {
    throw AppError.badRequest("Sıralama listesi bu görevin tüm alt görevlerini içermeli.");
  }

  await db.transaction(async (tx) => {
    for (let i = 0; i < data.itemIds.length; i++) {
      await tx
        .update(eventChecklistItems)
        .set({ sortOrder: i, updatedAt: new Date() })
        .where(eq(eventChecklistItems.id, data.itemIds[i]));
    }
  });

  return db
    .select()
    .from(eventChecklistItems)
    .where(eq(eventChecklistItems.eventId, eventId))
    .orderBy(asc(eventChecklistItems.sortOrder));
};
