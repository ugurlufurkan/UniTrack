import { and, asc, desc, eq, gte, inArray, lte, or, sql } from "drizzle-orm";

import { db } from "../../db";
import {
  courses,
  courseSchedule,
  eventNotifications,
  events,
  semesters,
} from "../../db/schema";
import {
  CreateEventDto,
  CreateScheduleDto,
  UpdateEventDto,
  UpdateScheduleDto,
} from "./calendar.types";
import { AppError } from "../../shared/errors/AppError";

const attachNotifications = async (
  eventRows: (typeof events.$inferSelect)[]
) => {
  if (!eventRows.length) return [];

  const ids = eventRows.map((e) => e.id);

  const notifications = await db
    .select()
    .from(eventNotifications)
    .where(inArray(eventNotifications.eventId, ids));

  const byEvent = new Map<string, typeof notifications>();
  for (const n of notifications) {
    const list = byEvent.get(n.eventId) ?? [];
    list.push(n);
    byEvent.set(n.eventId, list);
  }

  return eventRows.map((event) => ({
    ...event,
    notifications: byEvent.get(event.id) ?? [],
  }));
};

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

export const findAllEvents = async (
  userId: string,
  filters?: { startDate?: string; endDate?: string; type?: string }
) => {
  const conditions = [eq(events.userId, userId)];

  if (filters?.startDate) {
    conditions.push(gte(events.startAt, new Date(filters.startDate)));
  }
  if (filters?.endDate) {
    conditions.push(lte(events.startAt, new Date(filters.endDate)));
  }
  if (filters?.type) {
    conditions.push(eq(events.type, filters.type));
  }

  const rows = await db
    .select()
    .from(events)
    .where(and(...conditions))
    .orderBy(asc(events.startAt));

  return attachNotifications(rows);
};

export const findEventById = async (userId: string, eventId: string) => {
  const rows = await db
    .select()
    .from(events)
    .where(and(eq(events.id, eventId), eq(events.userId, userId)))
    .limit(1);

  if (!rows.length) {
    throw AppError.notFound("Etkinlik bulunamadı.");
  }

  const [withNotifications] = await attachNotifications(rows);
  return withNotifications;
};

export const findUpcomingEvents = async (userId: string, limit = 20) => {
  const now = new Date();

  const rows = await db
    .select()
    .from(events)
    .where(
      and(
        eq(events.userId, userId),
        gte(events.startAt, now),
        or(
          eq(events.status, "pending"),
          eq(events.status, "in_progress")
        )
      )
    )
    .orderBy(asc(events.startAt))
    .limit(limit);

  return attachNotifications(rows);
};

export const createEvent = async (userId: string, data: CreateEventDto) => {
  if (data.courseId) {
    await verifyCourseOwnership(userId, data.courseId);
  }

  return db.transaction(async (tx) => {
    const [event] = await tx
      .insert(events)
      .values({
        userId,
        courseId: data.courseId ?? null,
        title: data.title,
        description: data.description ?? null,
        type: data.type,
        startAt: new Date(data.startAt),
        endAt: data.endAt ? new Date(data.endAt) : null,
        location: data.location ?? null,
        priority: data.priority ?? "medium",
        status: data.status ?? "pending",
        color: data.color ?? "#6366F1",
        recurrence: data.recurrence ?? "none",
        notificationsEnabled: data.notificationsEnabled ?? true,
      })
      .returning();

    let notifications: (typeof eventNotifications.$inferSelect)[] = [];

    if (data.notifications?.length) {
      notifications = await tx
        .insert(eventNotifications)
        .values(
          data.notifications.map((n) => ({
            eventId: event.id,
            daysBefore: n.daysBefore,
            hoursBefore: n.hoursBefore,
            minutesBefore: n.minutesBefore,
          }))
        )
        .returning();
    }

    return { ...event, notifications };
  });
};

export const updateEvent = async (
  userId: string,
  eventId: string,
  data: UpdateEventDto
) => {
  const existing = await db
    .select()
    .from(events)
    .where(and(eq(events.id, eventId), eq(events.userId, userId)))
    .limit(1);

  if (!existing.length) {
    throw AppError.notFound("Etkinlik bulunamadı.");
  }

  const current = existing[0];

  if (data.courseId) {
    await verifyCourseOwnership(userId, data.courseId);
  }

  return db.transaction(async (tx) => {
    const [updated] = await tx
      .update(events)
      .set({
        courseId:
          data.courseId !== undefined ? data.courseId : current.courseId,
        title: data.title ?? current.title,
        description:
          data.description !== undefined
            ? data.description
            : current.description,
        type: data.type ?? current.type,
        startAt: data.startAt ? new Date(data.startAt) : current.startAt,
        endAt:
          data.endAt !== undefined
            ? data.endAt
              ? new Date(data.endAt)
              : null
            : current.endAt,
        location:
          data.location !== undefined ? data.location : current.location,
        priority: data.priority ?? current.priority,
        status: data.status ?? current.status,
        color: data.color ?? current.color,
        recurrence: data.recurrence ?? current.recurrence,
        notificationsEnabled:
          data.notificationsEnabled ?? current.notificationsEnabled,
        updatedAt: new Date(),
      })
      .where(eq(events.id, eventId))
      .returning();

    let notifications = await tx
      .select()
      .from(eventNotifications)
      .where(eq(eventNotifications.eventId, eventId));

    if (data.notifications) {
      await tx
        .delete(eventNotifications)
        .where(eq(eventNotifications.eventId, eventId));

      if (data.notifications.length) {
        notifications = await tx
          .insert(eventNotifications)
          .values(
            data.notifications.map((n) => ({
              eventId,
              daysBefore: n.daysBefore,
              hoursBefore: n.hoursBefore,
              minutesBefore: n.minutesBefore,
            }))
          )
          .returning();
      } else {
        notifications = [];
      }
    }

    return { ...updated, notifications };
  });
};

export const removeEvent = async (userId: string, eventId: string) => {
  const existing = await db
    .select()
    .from(events)
    .where(and(eq(events.id, eventId), eq(events.userId, userId)))
    .limit(1);

  if (!existing.length) {
    throw AppError.notFound("Etkinlik bulunamadı.");
  }

  await db.delete(events).where(eq(events.id, eventId));
};

export const findAllSchedule = async (userId: string) => {
  return db
    .select({
      id: courseSchedule.id,
      userId: courseSchedule.userId,
      courseId: courseSchedule.courseId,
      courseName: courses.name,
      dayOfWeek: courseSchedule.dayOfWeek,
      startTime: courseSchedule.startTime,
      endTime: courseSchedule.endTime,
      location: courseSchedule.location,
      createdAt: courseSchedule.createdAt,
    })
    .from(courseSchedule)
    .innerJoin(courses, eq(courseSchedule.courseId, courses.id))
    .where(eq(courseSchedule.userId, userId))
    .orderBy(asc(courseSchedule.dayOfWeek), asc(courseSchedule.startTime));
};

export const findTodaySchedule = async (userId: string) => {
  const today = new Date().getDay();

  return db
    .select({
      id: courseSchedule.id,
      courseId: courseSchedule.courseId,
      courseName: courses.name,
      dayOfWeek: courseSchedule.dayOfWeek,
      startTime: courseSchedule.startTime,
      endTime: courseSchedule.endTime,
      location: courseSchedule.location,
    })
    .from(courseSchedule)
    .innerJoin(courses, eq(courseSchedule.courseId, courses.id))
    .where(
      and(
        eq(courseSchedule.userId, userId),
        eq(courseSchedule.dayOfWeek, today)
      )
    )
    .orderBy(asc(courseSchedule.startTime));
};

export const createSchedule = async (
  userId: string,
  data: CreateScheduleDto
) => {
  await verifyCourseOwnership(userId, data.courseId);

  const [row] = await db
    .insert(courseSchedule)
    .values({
      userId,
      courseId: data.courseId,
      dayOfWeek: data.dayOfWeek,
      startTime: data.startTime,
      endTime: data.endTime,
      location: data.location ?? null,
    })
    .returning();

  return row;
};

export const updateSchedule = async (
  userId: string,
  scheduleId: string,
  data: UpdateScheduleDto
) => {
  const existing = await db
    .select()
    .from(courseSchedule)
    .where(
      and(eq(courseSchedule.id, scheduleId), eq(courseSchedule.userId, userId))
    )
    .limit(1);

  if (!existing.length) {
    throw AppError.notFound("Ders programı bulunamadı.");
  }

  const current = existing[0];

  if (data.courseId) {
    await verifyCourseOwnership(userId, data.courseId);
  }

  const [updated] = await db
    .update(courseSchedule)
    .set({
      courseId: data.courseId ?? current.courseId,
      dayOfWeek: data.dayOfWeek ?? current.dayOfWeek,
      startTime: data.startTime ?? current.startTime,
      endTime: data.endTime ?? current.endTime,
      location:
        data.location !== undefined ? data.location : current.location,
    })
    .where(eq(courseSchedule.id, scheduleId))
    .returning();

  return updated;
};

export const removeSchedule = async (userId: string, scheduleId: string) => {
  const existing = await db
    .select()
    .from(courseSchedule)
    .where(
      and(eq(courseSchedule.id, scheduleId), eq(courseSchedule.userId, userId))
    )
    .limit(1);

  if (!existing.length) {
    throw AppError.notFound("Ders programı bulunamadı.");
  }

  await db.delete(courseSchedule).where(eq(courseSchedule.id, scheduleId));
};

export const getCalendarSummary = async (userId: string) => {
  const now = new Date();
  const todayStart = new Date(now);
  todayStart.setHours(0, 0, 0, 0);
  const todayEnd = new Date(now);
  todayEnd.setHours(23, 59, 59, 999);

  const todayClasses = await findTodaySchedule(userId);

  const upcomingExams = await db
    .select({
      id: events.id,
      title: events.title,
      type: events.type,
      startAt: events.startAt,
      endAt: events.endAt,
      courseId: events.courseId,
      courseName: courses.name,
      color: events.color,
      status: events.status,
    })
    .from(events)
    .leftJoin(courses, eq(events.courseId, courses.id))
    .where(
      and(
        eq(events.userId, userId),
        eq(events.type, "exam"),
        gte(events.startAt, now),
        or(eq(events.status, "pending"), eq(events.status, "in_progress"))
      )
    )
    .orderBy(asc(events.startAt))
    .limit(5);

  const upcomingDeadlines = await db
    .select({
      id: events.id,
      title: events.title,
      type: events.type,
      startAt: events.startAt,
      endAt: events.endAt,
      courseId: events.courseId,
      courseName: courses.name,
      color: events.color,
      status: events.status,
    })
    .from(events)
    .leftJoin(courses, eq(events.courseId, courses.id))
    .where(
      and(
        eq(events.userId, userId),
        inArray(events.type, ["assignment", "project", "quiz"]),
        gte(events.startAt, now),
        or(eq(events.status, "pending"), eq(events.status, "in_progress"))
      )
    )
    .orderBy(asc(events.startAt))
    .limit(5);

  const overdueAssignments = await db
    .select({
      id: events.id,
      title: events.title,
      type: events.type,
      startAt: events.startAt,
      endAt: events.endAt,
      courseId: events.courseId,
      courseName: courses.name,
      color: events.color,
      status: events.status,
    })
    .from(events)
    .leftJoin(courses, eq(events.courseId, courses.id))
    .where(
      and(
        eq(events.userId, userId),
        inArray(events.type, ["assignment", "project", "quiz"]),
        lte(events.startAt, now),
        or(eq(events.status, "pending"), eq(events.status, "in_progress"))
      )
    )
    .orderBy(desc(events.startAt))
    .limit(5);

  const todayLessons = await db
    .select({
      id: events.id,
      title: events.title,
      type: events.type,
      startAt: events.startAt,
      endAt: events.endAt,
      courseId: events.courseId,
      courseName: courses.name,
      location: events.location,
      color: events.color,
    })
    .from(events)
    .leftJoin(courses, eq(events.courseId, courses.id))
    .where(
      and(
        eq(events.userId, userId),
        eq(events.type, "lesson"),
        gte(events.startAt, todayStart),
        lte(events.startAt, todayEnd)
      )
    )
    .orderBy(asc(events.startAt));

  return {
    todayClasses,
    todayLessons,
    upcomingExams,
    upcomingDeadlines,
    overdueAssignments,
  };
};
