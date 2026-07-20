import { GradeBand } from "../../db/schema";

export interface GradeResult {
  average: number | null;
  letterGrade: string | null;
  gradePoint: number | null;
  passed: boolean | null;
}

export interface GradeComponentInput {
  weight: number;
  score?: number | null;
}

// Sistem varsayilan harf notu skalasi (kullanici/ders icin ozel skala
// tanimlanmamissa bu kullanilir). "min" o harfi almak icin gereken
// en dusuk ortalamadir.
export const DEFAULT_GRADE_SCALE: GradeBand[] = [
  { letter: "AA", min: 90, point: 4.0 },
  { letter: "BA", min: 85, point: 3.5 },
  { letter: "BB", min: 80, point: 3.0 },
  { letter: "CB", min: 75, point: 2.5 },
  { letter: "CC", min: 70, point: 2.0 },
  { letter: "DC", min: 65, point: 1.5 },
  { letter: "DD", min: 60, point: 1.0 },
  { letter: "FD", min: 50, point: 0.5 },
  { letter: "FF", min: 0, point: 0 },
];

// Bir harfin "gecti" sayilmasi icin gereken en dusuk not katsayisi.
// (DD = 1.0 gecer, FD/FF gecmez; varsayilan skalayla eski davranisla birebir ayni.)
const PASSING_POINT_THRESHOLD = 1.0;

export function resolveGradeScale(
  courseScale?: GradeBand[] | null,
  userDefaultScale?: GradeBand[] | null
): GradeBand[] {
  const scale = courseScale ?? userDefaultScale ?? DEFAULT_GRADE_SCALE;
  return [...scale].sort((a, b) => b.min - a.min);
}

export function calculateGrade(
  components: GradeComponentInput[],
  scale: GradeBand[] = DEFAULT_GRADE_SCALE
): GradeResult {
  if (!components.length) {
    return { average: null, letterGrade: null, gradePoint: null, passed: null };
  }

  // Herhangi bir bilesenin puani girilmemisse ders "devam ediyor" sayilir;
  // GANO ve gecti/kaldi hesaplarinin disinda tutulur.
  const hasMissingScore = components.some(
    (c) => c.score === null || c.score === undefined
  );

  if (hasMissingScore) {
    return { average: null, letterGrade: null, gradePoint: null, passed: null };
  }

  const totalWeight = components.reduce((sum, c) => sum + c.weight, 0) || 100;

  const weightedSum = components.reduce(
    (sum, c) => sum + (c.score as number) * c.weight,
    0
  );

  const average = Number((weightedSum / totalWeight).toFixed(2));

  const sortedScale = [...scale].sort((a, b) => b.min - a.min);
  const band =
    sortedScale.find((b) => average >= b.min) ??
    sortedScale[sortedScale.length - 1];

  return {
    average,
    letterGrade: band.letter,
    gradePoint: band.point,
    passed: band.point >= PASSING_POINT_THRESHOLD,
  };
}
