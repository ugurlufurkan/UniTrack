import { z } from "zod";

// Kullanıcı /parse'dan dönen önizlemeyi düzenleyip (ders çıkarma, not
// düzeltme vb.) bu şemayla geri gönderir; biz de bu haliyle kaydederiz.
export const importCourseSchema = z.object({
  name: z.string().trim().min(1).max(150),
  credit: z.number().int().min(0).max(30),
  grade: z.string().trim().toUpperCase().min(1).max(4),
});

export const importSemesterSchema = z.object({
  year: z.number().int().min(2000).max(2100),
  term: z.enum(["Güz", "Bahar", "Yaz"]),
  courses: z.array(importCourseSchema).min(1).max(100),
});

export const importTranscriptSchema = z.object({
  semesters: z.array(importSemesterSchema).min(1).max(40),
});
