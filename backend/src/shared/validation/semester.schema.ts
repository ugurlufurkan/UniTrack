import { z } from "zod";

export const createSemesterSchema = z.object({
  year: z.number().int().min(2000).max(2100),

  term: z.string().min(1).max(20),
});