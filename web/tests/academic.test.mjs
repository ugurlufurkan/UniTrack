import { test } from 'node:test';
import assert from 'node:assert/strict';
import { calculateGpa, sortSemesters, localDate } from '../lib/academic.mjs';
import { resolveEndpoint, isSameOriginMutation } from '../lib/api-policy.mjs';

test('GPA excludes ongoing courses and includes zero-point failures', () => {
  const courses = [
    { id: 'a', credit: 3, gradePoint: 4 },
    { id: 'b', credit: 3, gradePoint: 0 },
    { id: 'c', credit: 6, gradePoint: null },
  ];
  assert.equal(calculateGpa(courses), 2);
  assert.equal(calculateGpa(courses, { c: 4 }), 3);
  assert.equal(calculateGpa(courses, { a: 0 }), 0);
  assert.equal(calculateGpa([]), 0);
});
test('semesters have a deterministic academic ordering', () => {
  assert.deepEqual(
    sortSemesters([
      { year: 2025, term: 'Yaz' },
      { year: 2026, term: 'Bahar' },
      { year: 2026, term: 'Güz' },
    ]).map((s) => s.term),
    ['Bahar', 'Güz', 'Yaz'],
  );
});
test('calendar dates use local days rather than UTC slicing', () => {
  const date = new Date(2026, 9, 15, 0, 10);
  assert.equal(localDate(date), '2026-10-15');
});
test('API proxy only exposes known routes and methods', () => {
  assert.equal(resolveEndpoint('auth/refresh', 'POST'), null);
  assert.equal(resolveEndpoint('../auth/login', 'POST'), null);
  assert.equal(resolveEndpoint('https://attacker.test', 'GET'), null);
  assert.equal(resolveEndpoint('courses', 'DELETE'), null);
  assert.equal(resolveEndpoint('auth/login', 'POST').public, true);
  assert.equal(resolveEndpoint('courses', 'POST').public, false);
  assert.equal(resolveEndpoint('content/pages/privacy', 'GET').public, true);
});
test('mutations require a same-origin request and application header', () => {
  const req = (origin, header) =>
    new Request('https://unitrack.test/api/student/courses', {
      method: 'POST',
      headers: { origin, 'x-unitrack-web': header },
    });
  assert.equal(isSameOriginMutation(req('https://unitrack.test', '1')), true);
  assert.equal(isSameOriginMutation(req('https://attacker.test', '1')), false);
  assert.equal(isSameOriginMutation(req('https://unitrack.test', '')), false);
});
