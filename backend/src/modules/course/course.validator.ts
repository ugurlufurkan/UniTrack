import { z } from "zod";
import { gradeScaleSchema } from "../../shared/validation/gradeScale.schema";

export const courseComponentSchema = z.object({
  id: z.string().uuid().nullable().optional(), // guncellemede mevcut bileseni esler
  name: z.string().trim()
    .min(1, "Bileşen adı boş bırakılamaz.")
    .max(100, "Bileşen adı çok uzun."),
  weight: z.number()
    .min(0, "Ağırlık sıfırdan küçük olamaz.")
    .max(100, "Ağırlık %100'den büyük olamaz."),
  score: z.number()
    .min(0, "Not sıfırdan küçük olamaz.")
    .max(100, "Not 100'den büyük olamaz.")
    .nullable().optional(),
});

const componentsRefinement = (
  components: z.infer<typeof courseComponentSchema>[]
) => {
  const totalWeight = components.reduce((sum, c) => sum + c.weight, 0);
  return Math.abs(totalWeight - 100) < 0.01;
};

export const createCourseSchema = z.object({
  semesterId: z.string().uuid(),
  name: z.string().trim()
    .min(2, "Ders adı en az 2 karakter olmalıdır.")
    .max(150, "Ders adı çok uzun."),
  credit: z.number()
    .min(1, "Kredi değeri en az 1 olmalıdır.")
    .max(30, "Kredi değeri çok yüksek."),
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
  name: z.string().trim()
    .min(2, "Ders adı en az 2 karakter olmalıdır.")
    .max(150, "Ders adı çok uzun.")
    .optional(),
  credit: z.number()
    .min(1, "Kredi değeri en az 1 olmalıdır.")
    .max(30, "Kredi değeri çok yüksek.")
    .optional(),
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
