import { z } from "zod";
import { gradeScaleSchema } from "../../shared/validation/gradeScale.schema";

export const courseComponentSchema = z.object({
  id: z.string().uuid().optional(), // guncellemede mevcut bileseni esler
  name: z.string().min(1).max(100),
  weight: z.number().min(0).max(100),
  score: z.number().min(0).max(100).nullable().optional(),
});

const componentsRefinement = (
  components: z.infer<typeof courseComponentSchema>[]
) => {
  const totalWeight = components.reduce((sum, c) => sum + c.weight, 0);
  return Math.abs(totalWeight - 100) < 0.01;
};

export const createCourseSchema = z.object({
  semesterId: z.string().uuid(),
  name: z.string().min(2).max(150),
  credit: z.number().min(1).max(30),
  components: z
    .array(courseComponentSchema)
    .min(1, "En az bir bileşen (Vize, Final, Proje vb.) eklemelisin.")
    .refine(
      componentsRefinement,
      "Bileşen ağırlıklarının toplamı %100 olmalı."
    ),
  gradeScale: gradeScaleSchema.nullable().optional(),
});

export const updateCourseSchema = z.object({
  name: z.string().min(2).max(150).optional(),
  credit: z.number().min(1).max(30).optional(),
  components: z
    .array(courseComponentSchema)
    .min(1, "En az bir bileşen (Vize, Final, Proje vb.) eklemelisin.")
    .refine(
      componentsRefinement,
      "Bileşen ağırlıklarının toplamı %100 olmalı."
    )
    .optional(),
  gradeScale: gradeScaleSchema.nullable().optional(),
});
