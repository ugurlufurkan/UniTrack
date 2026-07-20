import { z } from "zod";

export const gradeBandSchema = z.object({
  letter: z.string().min(1).max(4),
  min: z.number().min(0).max(100),
  point: z.number().min(0).max(4.5),
});

export const gradeScaleSchema = z
  .array(gradeBandSchema)
  .min(2, "En az 2 harf notu aralığı girilmeli.")
  .max(20)
  .refine(
    (bands) => bands.some((b) => b.min === 0),
    "Skalada 0 puandan başlayan bir aralık (en düşük harf) olmalı."
  )
  .refine((bands) => {
    const mins = bands.map((b) => b.min);
    return new Set(mins).size === mins.length;
  }, "Aynı minimum puana sahip iki aralık olamaz.");

export const gradeScaleUpdateSchema = z.object({
  gradeScale: gradeScaleSchema.nullable(),
});
