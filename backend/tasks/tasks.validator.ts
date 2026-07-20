import { z } from "zod";

const typeEnum = z.enum(["assignment", "project", "presentation", "other"]);
const priorityEnum = z.enum(["low", "medium", "high"]);
const statusEnum = z.enum(["pending", "completed"]);

export const createTaskSchema = z.object({
  courseId: z.string().uuid().nullable().optional(),
  title: z.string().min(1).max(200),
  description: z.string().max(2000).nullable().optional(),
  type: typeEnum.optional(),
  dueAt: z.string().datetime().nullable().optional(),
  priority: priorityEnum.optional(),
  checklist: z
    .array(z.object({ title: z.string().min(1).max(300) }))
    .max(50)
    .optional(),
});

export const updateTaskSchema = z.object({
  courseId: z.string().uuid().nullable().optional(),
  title: z.string().min(1).max(200).optional(),
  description: z.string().max(2000).nullable().optional(),
  type: typeEnum.optional(),
  dueAt: z.string().datetime().nullable().optional(),
  priority: priorityEnum.optional(),
  status: statusEnum.optional(),
});

export const createChecklistItemSchema = z.object({
  title: z.string().min(1).max(300),
});

export const updateChecklistItemSchema = z.object({
  title: z.string().min(1).max(300).optional(),
  isDone: z.boolean().optional(),
  sortOrder: z.number().int().min(0).optional(),
});
