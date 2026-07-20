import { z } from "zod";

export const createSemesterSchema = z.object({
  year: z.number().min(2020).max(2100),

  term: z.enum([
    "Güz",
    "Bahar",
    "Yaz"
  ])
});

// Update currently replaces year + term wholesale (no partial update support
// in semester.service yet), so it shares the same required shape as create.
export const updateSemesterSchema = createSemesterSchema;