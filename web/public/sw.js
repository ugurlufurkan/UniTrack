const CACHE = 'unitrack-public-v2';
const OFFLINE = [
  '/offline.html',
  '/offline.js',
  '/offline.css',
  '/icons/icon-192.png',
  '/icons/icon-512.png',
];
self.addEventListener('install', (event) => {
  event.waitUntil(
    caches
      .open(CACHE)
      .then((cache) => cache.addAll(OFFLINE))
      .then(() => self.skipWaiting()),
  );
});

function notificationUrl(value) {
  try {
    const url = new URL(typeof value === 'string' ? value : '/calendar', self.location.origin);
    if (url.origin === self.location.origin && !url.username && !url.password) return url.href;
  } catch {}
  return `${self.location.origin}/calendar`;
}
self.addEventListener('push', (event) => {
  event.waitUntil(
    (async () => {
      let payload;
      try {
        payload = event.data.json();
      } catch {
        return;
      }
      if (!payload || typeof payload !== 'object' || typeof payload.owner !== 'string') return;
      const allowed = await new Promise((resolve) => {
        const request = indexedDB.open('unitrack-device', 1);
        request.onupgradeneeded = () => request.result.createObjectStore('preferences');
        request.onerror = () => resolve(false);
        request.onsuccess = () => {
          const tx = request.result.transaction('preferences', 'readonly'),
            store = tx.objectStore('preferences');
          const owner = store.get('pushOwner'),
            enabled = store.get('pushEnabled');
          tx.oncomplete = () => {
            request.result.close();
            resolve(enabled.result === true && owner.result === payload.owner);
          };
          tx.onerror = () => {
            request.result.close();
            resolve(false);
          };
        };
      });
      if (!allowed) return;
      await self.registration.showNotification(String(payload.title || 'UniTrack').slice(0, 100), {
        body: String(payload.body || '').slice(0, 300),
        icon: '/icons/icon-192.png',
        badge: '/icons/icon-192.png',
        tag: String(payload.tag || 'unitrack'),
        renotify: false,
        data: { url: notificationUrl(payload.url) },
      });
    })(),
  );
});
self.addEventListener('notificationclick', (event) => {
  event.notification.close();
  event.waitUntil(
    (async () => {
      const url = notificationUrl(event.notification.data?.url);
      const clients = await self.clients.matchAll({ type: 'window', includeUncontrolled: true });
      const client = clients.find((item) => new URL(item.url).origin === self.location.origin);
      if (client) {
        await client.navigate(url);
        await client.focus();
      } else await self.clients.openWindow(url);
    })(),
  );
});
self.addEventListener('activate', (event) => {
  event.waitUntil(
    caches
      .keys()
      .then((keys) =>
        Promise.all(
          keys
            .filter((key) => key.startsWith('unitrack-public-') && key !== CACHE)
            .map((key) => caches.delete(key)),
        ),
      )
      .then(() => self.clients.claim()),
  );
});
self.addEventListener('fetch', (event) => {
  const url = new URL(event.request.url);
  if (
    url.origin !== self.location.origin ||
    event.request.method !== 'GET' ||
    url.pathname.startsWith('/api/')
  )
    return;
  if (event.request.mode === 'navigate') {
    event.respondWith(fetch(event.request).catch(() => caches.match('/offline.html')));
  } else if (url.pathname.startsWith('/_next/static/') || OFFLINE.includes(url.pathname)) {
    event.respondWith(
      caches.open(CACHE).then(async (cache) => {
        const cached = await cache.match(event.request);
        if (cached) return cached;
        const response = await fetch(event.request);
        if (response.ok && response.type === 'basic')
          await cache.put(event.request, response.clone());
        return response;
      }),
    );
  }
});
