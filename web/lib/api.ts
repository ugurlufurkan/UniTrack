'use client';
import {
  clearDeviceData,
  markPendingLogout,
  hasPendingLogout,
  flushPendingLogout,
  clearPendingLogout,
} from './device-storage';

export class ApiError extends Error {
  constructor(
    message: string,
    public status: number,
  ) {
    super(message);
  }
}
export const SESSION_EXPIRED_EVENT = 'unitrack:session-expired';

let refreshing: Promise<boolean> | null = null;
async function refreshSession() {
  if (!refreshing) {
    const operation = async () => {
      // A different tab may have renewed the shared cookies while this tab waited.
      const check = await fetch('/api/student/auth/me', { cache: 'no-store' });
      if (check.ok) return true;
      if (check.status !== 401) throw new ApiError('Sunucuya ulaşılamıyor.', 503);
      const response = await fetch('/api/session/refresh', {
        method: 'POST',
        headers: { 'X-UniTrack-Web': '1' },
      });
      if (response.status >= 500)
        throw new ApiError('Oturum servisine ulaşılamıyor. Tekrar dene.', 503);
      return response.ok;
    };
    refreshing = (async () => {
      if (navigator.locks) return await navigator.locks.request('unitrack-session', operation);
      return await operation();
    })().finally(() => {
      refreshing = null;
    });
  }
  return refreshing;
}

export async function api<T>(path: string, options: RequestInit = {}): Promise<T> {
  const isPublic =
    /^auth\/(login|register|google|forgot-password|reset-password|verify-email)$/.test(path);
  if (hasPendingLogout() && path !== 'auth/logout') {
    if (!(await flushPendingLogout()))
      throw new ApiError('Çıkışın tamamlanması için bağlantını kontrol et.', 503);
    if (!isPublic) throw new ApiError('Oturum kapatıldı. Tekrar giriş yap.', 401);
  }
  if (
    ['auth/logout', 'auth/logout-all', 'auth/change-password'].includes(path) ||
    (path === 'auth/me' && options.method === 'DELETE')
  )
    await clearDeviceData();
  if (path === 'auth/logout') markPendingLogout();
  const headers = new Headers(options.headers);
  if (options.body && !(options.body instanceof FormData))
    headers.set('Content-Type', 'application/json');
  headers.set('X-UniTrack-Web', '1');
  const send = () => fetch(`/api/student/${path}`, { ...options, headers, cache: 'no-store' });
  let response: Response;
  try {
    response = await send();
    if (response.status === 401 && !isPublic && path !== 'auth/logout' && (await refreshSession()))
      response = await send();
  } catch (error) {
    if (error instanceof ApiError) throw error;
    if (error instanceof DOMException && error.name === 'AbortError') throw error;
    throw new ApiError('Bağlantı kurulamadı. İnternet bağlantını kontrol et.', 503);
  }
  if (path === 'auth/logout' && response.status < 500) clearPendingLogout();
  if (response.ok && /^auth\/(login|register|google)$/.test(path)) await clearDeviceData();
  if (response.status === 204) return undefined as T;
  const data = await response.json();
  if (!response.ok) {
    if (response.status === 401 && !/^auth\//.test(path))
      window.dispatchEvent(new Event(SESSION_EXPIRED_EVENT));
    const validation = data.errors?.[0]?.message;
    throw new ApiError(validation || data.message || 'İşlem tamamlanamadı.', response.status);
  }
  return data as T;
}

export const json = (value: unknown) => JSON.stringify(value);
