export const DEFAULT_SCALE = [
  { letter: 'AA', min: 90, point: 4 },
  { letter: 'BA', min: 85, point: 3.5 },
  { letter: 'BB', min: 80, point: 3 },
  { letter: 'CB', min: 75, point: 2.5 },
  { letter: 'CC', min: 70, point: 2 },
  { letter: 'DC', min: 65, point: 1.5 },
  { letter: 'DD', min: 60, point: 1 },
  { letter: 'FD', min: 50, point: 0.5 },
  { letter: 'FF', min: 0, point: 0 },
];

export function calculateGpa(courses, overrides = {}) {
  const graded = courses.filter((c) => overrides[c.id] !== undefined || c.gradePoint !== null);
  const credit = graded.reduce((sum, c) => sum + c.credit, 0);
  return credit
    ? graded.reduce((sum, c) => sum + c.credit * (overrides[c.id] ?? c.gradePoint), 0) / credit
    : 0;
}

export function semesterLabel(semester) {
  return `${semester.year}–${semester.year + 1} ${semester.term}`;
}
export function sortSemesters(semesters) {
  const terms = { Güz: 0, Bahar: 1, Yaz: 2 };
  return [...semesters].sort(
    (a, b) => b.year - a.year || (terms[b.term] ?? 0) - (terms[a.term] ?? 0),
  );
}

/** @param {Date | string} [value] */
export function localDate(value = new Date()) {
  const date = new Date(value);
  return `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, '0')}-${String(date.getDate()).padStart(2, '0')}`;
}
