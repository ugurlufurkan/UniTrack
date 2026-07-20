import { z } from "zod";

export const createChecklistItemSchema = z.object({
  title: z.string().min(1).max(200),
});

export const updateChecklistItemSchema = z.object({
  title: z.string().min(1).max(200).optional(),
  isDone: z.boolean().optional(),
  sortOrder: z.number().int().min(0).optional(),
});

export const reorderChecklistSchema = z.object({
  itemIds: z.array(z.string().uuid()).min(1),
});
