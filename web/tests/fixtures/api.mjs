// Isolated in-memory API for automated checks and browser QA. Never used by the app by default.
import http from 'node:http';
import { randomUUID } from 'node:crypto';
import { pathToFileURL } from 'node:url';
import { DEFAULT_SCALE, calculateGpa } from '../../lib/academic.mjs';

export function createFixture() {
  const semesterId = '11111111-1111-4111-8111-111111111111';
  const semesters = [{ id: semesterId, year: 2026, term: 'Güz' }];
  const courses = [
    ['Veri Yapıları ve Algoritmalar', 4, 86, 'BA', 3.5],
    ['Olasılık ve İstatistik', 3, 92, 'AA', 4],
    ['İşletim Sistemleri', 4, null, null, null],
    ['Veritabanı Yönetim Sistemleri', 3, 82, 'BB', 3],
    ['Yazılım Mühendisliği', 3, null, null, null],
    ['Bilgisayar Ağları', 3, 78, 'CB', 2.5],
  ].map(([name, credit, average, letterGrade, gradePoint], i) => ({
    id: `22222222-2222-4222-8222-${String(i + 1).padStart(12, '0')}`,
    semesterId,
    name,
    credit,
    average,
    letterGrade,
    gradePoint,
    passed: average === null ? null : true,
    gradeScale: null,
    components: [
      { name: 'Vize', weight: 40, score: average },
      { name: 'Final', weight: 60, score: average },
    ],
  }));
  const now = new Date();
  const start = new Date(now.getFullYear(), now.getMonth(), now.getDate() + 1, 14, 0).toISOString();
  const events = [
    {
      id: '33333333-3333-4333-8333-333333333333',
      title: 'Veri yapıları proje teslimi',
      type: 'project',
      description: 'Arama algoritmaları karşılaştırması ve rapor teslimi.',
      startAt: start,
      endAt: null,
      priority: 'high',
      status: 'pending',
      courseId: courses[0].id,
      checklist: [],
    },
    {
      id: '44444444-4444-4444-8444-444444444444',
      title: 'İstatistik çalışma soruları',
      type: 'assignment',
      description: 'İkinci bölüm uygulama sorularını tamamla.',
      startAt: start,
      endAt: null,
      priority: 'medium',
      status: 'in_progress',
      courseId: courses[1].id,
      checklist: [],
    },
  ];
  const schedule = [0, 1, 2].map((n) => ({
    id: `55555555-5555-4555-8555-${String(n + 1).padStart(12, '0')}`,
    courseId: courses[n].id,
    courseName: courses[n].name,
    dayOfWeek: now.getDay(),
    startTime: ['09:00', '11:00', '14:00'][n],
    endTime: ['10:50', '12:50', '15:50'][n],
    location: ['Mühendislik · A-201', 'Fen Fakültesi · B-104', 'Mühendislik · A-302'][n],
  }));
  const settings = {
    themePreference: 'LIGHT',
    targetGpa: 3.5,
    examPeriodStart: null,
    examPeriodEnd: null,
  };
  const attendance = courses.map((c) => ({
    courseId: c.id,
    courseName: c.name,
    totalWeeks: 14,
    weeklyHours: 3,
    attendanceLimitHours: 12,
    absentHours: 3,
    attendedHours: 9,
    excusedHours: 0,
    remainingAllowedHours: 9,
    absenceRatePercent: 7.14,
    isAtRisk: false,
    records: [],
  }));
  const state = {
    refreshCount: 0,
    failLogout: false,
    failRefresh: false,
    refreshStatus: 0,
    requests: [],
    access: 'test-access',
    refresh: 'test-refresh',
  };
  const server = http.createServer(async (req, res) => {
    const path = new URL(req.url, 'http://fixture.test').pathname.replace('/api/v1/', '');
    const chunks = [];
    for await (const chunk of req) chunks.push(chunk);
    let body = {};
    try {
      body = JSON.parse(Buffer.concat(chunks).toString() || '{}');
    } catch {
      /* Multipart parse is mocked below. */
    }
    state.requests.push({
      path,
      method: req.method,
      authorization: req.headers.authorization,
      body,
    });
    const send = (data, status = 200) => {
      res.writeHead(status, { 'Content-Type': 'application/json' });
      res.end(JSON.stringify(data));
    };
    if (path === 'auth/login' || path === 'auth/register' || path === 'auth/google') {
      if (body.password === 'wrong') return send({ message: 'E-posta veya şifre hatalı.' }, 401);
      return send({
        success: true,
        user: { name: 'Deniz Yılmaz', email: 'student@example.test' },
        accessToken: state.access,
        refreshToken: state.refresh,
      });
    }
    if (path === 'auth/refresh') {
      state.refreshCount++;
      if (state.refreshStatus)
        return send({ message: 'Refresh fixture error' }, state.refreshStatus);
      if (state.failRefresh) return send({ message: 'Temporarily unavailable' }, 503);
      if (body.refreshToken !== state.refresh) return send({ message: 'Expired' }, 401);
      state.access = 'test-renewed-access';
      state.refresh = 'test-renewed-refresh';
      return send({ accessToken: state.access, refreshToken: state.refresh });
    }
    if (path.startsWith('content/pages/'))
      return send({
        title: 'Test gizlilik metni',
        content: '## Test içeriği\n\nBu metin sadece test ortamında kullanılır.',
        updatedAt: now.toISOString(),
      });
    if (['auth/forgot-password', 'auth/reset-password', 'auth/verify-email'].includes(path))
      return send({ success: true, message: 'İşlem tamamlandı.' });
    if (req.headers.authorization !== `Bearer ${state.access}`)
      return send({ message: 'Giriş gerekli.' }, 401);
    if (path === 'auth/me' && req.method === 'GET')
      return send({
        user: {
          userId: 'student',
          name: 'Deniz Yılmaz',
          email: 'student@example.test',
          isEmailVerified: true,
        },
      });
    if (path === 'auth/logout')
      return send({ success: !state.failLogout }, state.failLogout ? 503 : 200);
    if (path === 'auth/me' && req.method === 'DELETE') {
      res.writeHead(204);
      return res.end();
    }
    if (path === 'auth/devices')
      return send({
        devices: [
          {
            id: 'device-1',
            deviceName: 'Test tarayıcısı',
            deviceType: 'desktop',
            lastUsedAt: now.toISOString(),
            createdAt: now.toISOString(),
          },
        ],
      });
    if (path === 'settings') {
      if (req.method === 'PATCH') Object.assign(settings, body);
      return send(settings);
    }
    if (path === 'grade-scale/default') return send({ gradeScale: DEFAULT_SCALE, isCustom: false });
    if (path === 'dashboard')
      return send({
        totalSemesters: semesters.length,
        totalCourses: courses.length,
        totalCredits: 13,
        passedCourses: 4,
        failedCourses: 0,
        ongoingCourses: 2,
        gpa: calculateGpa(courses),
        weeklyLessonCount: schedule.length,
        attendanceOverview: { atRiskCourses: 0 },
      });
    if (path === 'statistics')
      return send({
        totalCourses: courses.length,
        totalCredits: 20,
        overallAverage: 84.5,
        passedCourses: 4,
        failedCourses: 0,
        ongoingCourses: 2,
        semesterGpa: [{ semesterId, semester: 'Güz 2026', gpa: calculateGpa(courses) }],
      });
    if (path === 'attendance/courses') return send(attendance);
    const attendanceMatch = /^attendance\/courses\/([^/]+)$/.exec(path);
    if (attendanceMatch) return send(attendance.find((c) => c.courseId === attendanceMatch[1]));
    if (path === 'content/announcements' || path === 'content/faqs' || path === 'content/tips')
      return send([]);
    if (path === 'tasks')
      return send(events.filter((e) => ['assignment', 'project', 'presentation'].includes(e.type)));
    if (path === 'export') return send({ user: { name: 'Test student' }, semesters, courses });
    if (path === 'transcript-import/parse')
      return send({
        semesters: [
          {
            year: 2025,
            term: 'Bahar',
            courses: [{ name: 'Örnek ders', credit: 3, grade: 'AA', recognized: true }],
          },
        ],
      });
    if (path === 'transcript-import/import') return send({ success: true });
    const checklist = /^tasks\/([^/]+)\/checklist$/.exec(path);
    if (checklist) {
      const item = { id: randomUUID(), ...body, isDone: false };
      events.find((e) => e.id === checklist[1]).checklist.push(item);
      return send(item, 201);
    }
    const taskMatch = /^tasks\/([^/]+)$/.exec(path);
    if (taskMatch) return send(events.find((e) => e.id === taskMatch[1]));
    for (const [prefix, list] of [
      ['semesters', semesters],
      ['courses', courses],
      ['calendar/events', events],
      ['calendar/schedule', schedule],
    ]) {
      if (path === prefix && req.method === 'GET') return send(list);
      if (path === prefix && req.method === 'POST') {
        const item = { id: randomUUID(), ...body };
        list.push(item);
        return send(item, 201);
      }
      if (path.startsWith(`${prefix}/`)) {
        const id = path.slice(prefix.length + 1);
        const index = list.findIndex((item) => item.id === id);
        if (index < 0) return send({ message: 'Bulunamadı.' }, 404);
        if (req.method === 'PUT') Object.assign(list[index], body);
        if (req.method === 'DELETE') list.splice(index, 1);
        return send({ success: true });
      }
    }
    return send({ success: true });
  });
  return { server, state };
}

if (process.argv[1] && import.meta.url === pathToFileURL(process.argv[1]).href) {
  createFixture().server.listen(Number(process.env.FIXTURE_PORT || 5055), '127.0.0.1', () =>
    console.log('TEST FIXTURE ONLY: http://127.0.0.1:5055/api/v1'),
  );
}
