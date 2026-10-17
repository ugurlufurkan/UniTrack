import { test, expect, type Page } from '@playwright/test';

test('student can register, manage academic data, sign out and sign in again', async ({ page }) => {
  const errors: string[] = [];
  page.on('pageerror', (error) => errors.push(error.message));
  const email = `student-${Date.now()}@example.invalid`;
  await page.goto('/register');
  await page.getByLabel('Ad soyad').fill('Test Öğrenci');
  await page.getByLabel('E-posta adresi').fill(email);
  await page.getByLabel('Şifre', { exact: true }).fill('UniTrackTest123!');
  await page.getByLabel('Şifre tekrar', { exact: true }).fill('UniTrackTest123!');
  await page.getByRole('checkbox').check();
  await page.getByRole('button', { name: 'Hesabımı oluştur' }).click();
  await expect(page).toHaveURL('/');
  await page.getByRole('link', { name: 'Dönemlerim', exact: true }).click();
  await page.getByRole('button', { name: 'Dönem ekle' }).click();
  await page.getByLabel('Akademik yıl başlangıcı').fill('2026');
  await page.getByRole('dialog').getByRole('button', { name: 'Kaydet', exact: true }).click();
  await expect(page.getByText('Güz dönemi', { exact: true })).toBeVisible();
  await page.getByRole('link', { name: 'Dersler ve notlar', exact: true }).click();
  await page.getByRole('button', { name: 'Ders ekle', exact: true }).click();
  await page.getByLabel('Ders adı', { exact: true }).fill('Veri Yapıları');
  await page.getByLabel('Kredi', { exact: true }).fill('3');
  await page.getByLabel('Vize notu').fill('80');
  await page.getByLabel('Final notu').fill('90');
  await page.getByRole('dialog').getByRole('button', { name: 'Kaydet', exact: true }).click();
  await expect(page.getByText('Veri Yapıları', { exact: true })).toBeVisible();
  await page.reload();
  await expect(page.getByText('Veri Yapıları', { exact: true })).toBeVisible();
  await page.goto('/settings');
  await page.getByRole('checkbox', { name: 'Çevrimdışı ders özeti sakla' }).check();
  await expect(page.getByRole('status')).toContainText('Çevrimdışı ders kopyası hazır.');
  await page.evaluate(async () => {
    await navigator.serviceWorker.ready;
    if (!navigator.serviceWorker.controller)
      await new Promise((resolve) =>
        navigator.serviceWorker.addEventListener('controllerchange', resolve, { once: true }),
      );
  });
  await page.context().setOffline(true);
  await page.goto('/courses');
  await expect(page.getByRole('heading', { name: 'Bağlantına kısa bir ara.' })).toBeVisible();
  await expect(page.getByText('Veri Yapıları', { exact: true })).toBeVisible();
  await page.context().setOffline(false);
  await page.goto('/courses');
  const cached = await page.evaluate(async () => {
    const urls: string[] = [];
    for (const name of await caches.keys())
      for (const request of await (await caches.open(name)).keys())
        urls.push(new URL(request.url).pathname);
    return urls;
  });
  expect(cached.some((url) => url.startsWith('/api/') || url === '/login')).toBe(false);
  await page.setViewportSize({ width: 390, height: 844 });
  const dimensions = await page.evaluate(() => ({
    width: innerWidth,
    document: document.documentElement.scrollWidth,
    outside: [...document.querySelectorAll('body *')]
      .map((el) => ({
        tag: el.tagName,
        class: el.className,
        right: el.getBoundingClientRect().right,
      }))
      .filter((el) => el.right > innerWidth + 1)
      .slice(0, 12),
  }));
  expect(dimensions, JSON.stringify(dimensions)).toMatchObject({ width: 390, document: 390 });
  await page.getByRole('button', { name: 'Menüyü aç', exact: true }).click();
  await page.getByRole('button', { name: 'Çıkış yap', exact: true }).click();
  await expect(page).toHaveURL('/login');
  await page.getByLabel('E-posta adresi').fill(email);
  await page.getByLabel('Şifre', { exact: true }).fill('UniTrackTest123!');
  await page.getByRole('button', { name: 'Giriş yap' }).click();
  await expect(page).toHaveURL('/');
  expect(errors).toEqual([]);
});

test('push consent binds reminders to this account and rejects foreign payloads', async ({
  page,
  context,
}) => {
  await context.grantPermissions(['notifications'], { origin: 'http://127.0.0.1:3198' });
  await page.addInitScript(() => {
    if (!('PushManager' in window)) return;
    let current: PushSubscription | null = null;
    PushManager.prototype.getSubscription = async () => current;
    PushManager.prototype.subscribe = async (options) => {
      const pair = await crypto.subtle.generateKey({ name: 'ECDH', namedCurve: 'P-256' }, true, [
        'deriveBits',
      ]);
      const encode = (buffer: ArrayBuffer) =>
        btoa(String.fromCharCode(...new Uint8Array(buffer)))
          .replace(/=/g, '')
          .replace(/\+/g, '-')
          .replace(/\//g, '_');
      const data = {
        endpoint: `https://fcm.googleapis.com/fcm/send/fixture-${crypto.randomUUID()}`,
        keys: {
          p256dh: encode(await crypto.subtle.exportKey('raw', pair.publicKey)),
          auth: encode(crypto.getRandomValues(new Uint8Array(16)).buffer),
        },
      };
      current = {
        endpoint: data.endpoint,
        options: { applicationServerKey: options?.applicationServerKey as ArrayBuffer },
        toJSON: () => data,
        unsubscribe: async () => {
          current = null;
          return true;
        },
      } as unknown as PushSubscription;
      return current;
    };
  });
  await page.goto('/register');
  await page.getByLabel('Ad soyad').fill('Push Öğrenci');
  await page.getByLabel('E-posta adresi').fill(`push-${Date.now()}@example.invalid`);
  await page.getByLabel('Şifre', { exact: true }).fill('UniTrackTest123!');
  await page.getByLabel('Şifre tekrar', { exact: true }).fill('UniTrackTest123!');
  await page.getByRole('checkbox').check();
  await page.getByRole('button', { name: 'Hesabımı oluştur' }).click();
  await expect(page).toHaveURL('/');
  await page.goto('/settings');
  await page.getByRole('button', { name: 'Hatırlatmaları aç', exact: true }).click();
  await expect(page.getByRole('status')).toContainText('Bu cihazdaki hatırlatmalar açıldı.');
  const owner = await page.evaluate(
    () =>
      new Promise<string>((resolve, reject) => {
        const open = indexedDB.open('unitrack-device', 1);
        open.onerror = () => reject(new Error('Device store unavailable'));
        open.onsuccess = () => {
          const tx = open.result.transaction('preferences');
          const read = tx.objectStore('preferences').get('pushOwner');
          tx.oncomplete = () => {
            open.result.close();
            resolve(read.result);
          };
        };
      }),
  );
  const worker = context.serviceWorkers().find((item) => item.url().endsWith('/sw.js'))!;
  const notices = await worker.evaluate(async (owner) => {
    type Notice = { title: string; url: string };
    const scope = globalThis as unknown as {
      notices: Notice[];
      registration: {
        showNotification: (title: string, options: { data: { url: string } }) => Promise<void>;
      };
      PushEvent: new (type: string, options: { data: string }) => Event;
      dispatchEvent: (event: Event) => boolean;
    };
    scope.notices = [];
    scope.registration.showNotification = async (title, options) => {
      scope.notices.push({ title, url: options.data.url });
    };
    for (const identity of ['another-account', owner]) {
      const event = new scope.PushEvent('push', {
        data: JSON.stringify({
          owner: identity,
          title: 'Reminder',
          url: 'https://outside.example/phish',
        }),
      });
      const pending: Promise<unknown>[] = [];
      Object.defineProperty(event, 'waitUntil', {
        value: (promise: Promise<unknown>) => pending.push(promise),
      });
      scope.dispatchEvent(event);
      await Promise.all(pending);
    }
    return scope.notices;
  }, owner);
  expect(notices).toEqual([{ title: 'Reminder', url: 'http://127.0.0.1:3198/calendar' }]);
  await page.getByRole('button', { name: 'Hatırlatmaları kapat', exact: true }).click();
  await expect(page.getByRole('status')).toContainText('Bu cihazdaki hatırlatmalar kapatıldı.');
});

test('offline sign-out clears the device copy and finishes server logout on reconnect', async ({
  page,
  context,
}) => {
  await page.goto('/register');
  await page.getByLabel('Ad soyad').fill('Offline Öğrenci');
  await page.getByLabel('E-posta adresi').fill(`offline-${Date.now()}@example.invalid`);
  await page.getByLabel('Şifre', { exact: true }).fill('UniTrackTest123!');
  await page.getByLabel('Şifre tekrar', { exact: true }).fill('UniTrackTest123!');
  await page.getByRole('checkbox').check();
  await page.getByRole('button', { name: 'Hesabımı oluştur' }).click();
  await expect(page).toHaveURL('/');
  const refresh = (await context.cookies()).find(
    (cookie) => cookie.name === 'unitrack_refresh',
  )!.value;
  await page.goto('/settings');
  await page.getByRole('checkbox', { name: 'Çevrimdışı ders özeti sakla' }).check();
  await expect(page.getByRole('status')).toContainText('Çevrimdışı ders kopyası hazır.');
  await page.evaluate(async () => {
    await navigator.serviceWorker.ready;
  });
  await context.setOffline(true);
  await page.getByRole('button', { name: 'Çıkış yap', exact: true }).click();
  await expect
    .poll(() => page.evaluate(() => localStorage.getItem('unitrack:logout-pending')))
    .toBe('1');
  await context.setOffline(false);
  await page.goto('/login');
  await expect
    .poll(async () =>
      (await context.cookies()).some((cookie) => cookie.name === 'unitrack_refresh'),
    )
    .toBe(false);
  await page.goto('/');
  await expect(page).toHaveURL('/login');
  const invalidated = await page.request.post('http://127.0.0.1:5105/api/v1/auth/refresh', {
    data: { refreshToken: refresh },
  });
  expect(invalidated.status()).toBe(401);
});

test('student accounts cannot read or modify each others academic records or sessions', async ({
  page,
  browser,
}) => {
  const otherContext = await browser.newContext({
    extraHTTPHeaders: { 'x-forwarded-for': '203.0.113.21' },
  });
  const other = await otherContext.newPage();
  const call = (target: Page, path: string, method = 'GET', body?: unknown) =>
    target.evaluate(
      async ({ path, method, body }) => {
        const response = await fetch(`/api/student/${path}`, {
          method,
          headers: { 'Content-Type': 'application/json', 'X-UniTrack-Web': '1' },
          ...(body === undefined ? {} : { body: JSON.stringify(body) }),
        });
        return {
          status: response.status,
          body: response.status === 204 ? null : await response.json(),
        };
      },
      { path, method, body },
    );
  try {
    await page.goto('/register');
    await other.goto('/register');
    for (const [target, name] of [
      [page, 'Owner'],
      [other, 'Stranger'],
    ] as const) {
      const registered = await call(target, 'auth/register', 'POST', {
        name,
        email: `isolation-${name}-${Date.now()}@example.invalid`,
        password: 'UniTrackTest123!',
      });
      expect(registered.status).toBe(200);
    }
    const semester = await call(page, 'semesters', 'POST', { year: 2026, term: 'Güz' });
    const input = {
      semesterId: semester.body.id,
      name: 'Account isolation course',
      credit: 3,
      components: [{ name: 'Final', weight: 100, score: 80 }],
    };
    const course = await call(page, 'courses', 'POST', input);
    expect(course.status).toBe(201);
    const event = await call(page, 'calendar/events', 'POST', {
      title: 'Private assignment',
      type: 'project',
      startAt: new Date(Date.now() + 3600000).toISOString(),
    });
    const checklist = await call(page, `tasks/${event.body.id}/checklist`, 'POST', {
      title: 'Private subtask',
    });
    const devices = await call(page, 'auth/devices');
    expect((await call(other, 'courses')).body).toEqual([]);
    expect((await call(other, 'courses', 'POST', input)).status).toBe(400);
    expect(
      (await call(other, `courses/${course.body.id}`, 'PUT', { name: 'Hijacked course' })).status,
    ).toBe(404);
    expect((await call(other, `semesters/${semester.body.id}`, 'DELETE')).status).toBe(404);
    expect((await call(other, `attendance/courses/${course.body.id}`)).status).toBe(404);
    expect(
      (await call(other, `calendar/events/${event.body.id}`, 'PUT', { status: 'completed' }))
        .status,
    ).toBe(404);
    expect(
      (await call(other, `tasks/checklist/${checklist.body.id}`, 'PUT', { isDone: true })).status,
    ).toBe(404);
    expect((await call(other, `auth/devices/${devices.body.devices[0].id}`, 'DELETE')).status).toBe(
      404,
    );
    const exported = await call(other, 'export');
    expect(exported.status).toBe(200);
    expect(JSON.stringify(exported.body)).not.toContain('Private subtask');
    const retained = await call(page, 'courses');
    expect(retained.body[0].name).toBe(input.name);
    expect((await call(page, 'auth/me', 'DELETE')).status).toBe(204);
    expect((await call(other, 'auth/me', 'DELETE')).status).toBe(204);
  } finally {
    await otherContext.close();
  }
});

test('signing out also removes private screens from another open tab', async ({
  page,
  context,
}) => {
  await page.goto('/register');
  await page.getByLabel('Ad soyad').fill('Tabs Öğrenci');
  await page.getByLabel('E-posta adresi').fill(`tabs-${Date.now()}@example.invalid`);
  await page.getByLabel('Şifre', { exact: true }).fill('UniTrackTest123!');
  await page.getByLabel('Şifre tekrar', { exact: true }).fill('UniTrackTest123!');
  await page.getByRole('checkbox').check();
  await page.getByRole('button', { name: 'Hesabımı oluştur' }).click();
  await expect(page).toHaveURL('/');
  const other = await context.newPage();
  try {
    await other.goto('/');
    await expect(other.getByRole('heading', { name: /Merhaba/ })).toBeVisible();
    await page.getByRole('button', { name: 'Çıkış yap', exact: true }).click();
    await expect(page).toHaveURL('/login');
    await expect(other).toHaveURL(/\/login/);
    await expect(other.getByRole('heading', { name: /Merhaba/ })).toHaveCount(0);
  } finally {
    await other.close();
  }
});
