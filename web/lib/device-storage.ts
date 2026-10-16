'use client';
import type { Course, Semester, User } from './types';

const NAME = 'unitrack-device';
const STORE = 'preferences';
const LOGOUT = 'unitrack:logout-pending';
export type OfflineSnapshot = {
  owner: string;
  savedAt: number;
  semesters: Pick<Semester, 'id' | 'year' | 'term'>[];
  courses: Pick<Course, 'name' | 'credit' | 'letterGrade' | 'semesterId'>[];
};

async function database(): Promise<IDBDatabase> {
  return new Promise((resolve, reject) => {
    const request = indexedDB.open(NAME, 1);
    request.onupgradeneeded = () => request.result.createObjectStore(STORE);
    request.onsuccess = () => {
      request.result.onversionchange = () => request.result.close();
      resolve(request.result);
    };
    request.onerror = () => reject(new Error('Bu tarayıcıda cihaz depolaması kullanılamıyor.'));
  });
}
async function transaction<T>(
  mode: IDBTransactionMode,
  operation: (store: IDBObjectStore) => IDBRequest<T> | void,
): Promise<T | undefined> {
  const db = await database();
  return new Promise((resolve, reject) => {
    const tx = db.transaction(STORE, mode);
    const request = operation(tx.objectStore(STORE));
    tx.oncomplete = () => {
      const result = request?.result;
      db.close();
      resolve(result);
    };
    tx.onerror = tx.onabort = () => {
      db.close();
      reject(new Error('Cihazdaki kopya güncellenemedi.'));
    };
  });
}
export async function offlineStatus() {
  const enabled = await transaction<boolean>('readonly', (store) => store.get('enabled'));
  const snapshot = await transaction<OfflineSnapshot>('readonly', (store) => store.get('snapshot'));
  return { enabled: enabled === true, snapshot };
}
export async function setOfflineEnabled(enabled: boolean) {
  await transaction('readwrite', (store) => {
    store.put(enabled, 'enabled');
    if (!enabled) store.delete('snapshot');
  });
}
export async function saveOfflineSnapshot(user: User, semesters: Semester[], courses: Course[]) {
  const snapshot: OfflineSnapshot = {
    owner: user.userId,
    savedAt: Date.now(),
    semesters: semesters.map(({ id, year, term }) => ({ id, year, term })),
    courses: courses.slice(0, 5000).map(({ name, credit, letterGrade, semesterId }) => ({
      name,
      credit,
      letterGrade,
      semesterId,
    })),
  };
  await transaction('readwrite', (store) => {
    const enabled = store.get('enabled');
    enabled.onsuccess = () => {
      if (enabled.result === true) store.put(snapshot, 'snapshot');
    };
  });
}
export async function clearDeviceData() {
  try {
    await transaction('readwrite', (store) => {
      store.clear();
    });
  } catch {
    /* Storage may be disabled entirely. */
  }
}
export function markPendingLogout() {
  try {
    localStorage.setItem(LOGOUT, '1');
  } catch {}
}
export function hasPendingLogout() {
  try {
    return localStorage.getItem(LOGOUT) === '1';
  } catch {
    return false;
  }
}
export function clearPendingLogout() {
  try {
    localStorage.removeItem(LOGOUT);
  } catch {}
}
let flushing: Promise<boolean> | null = null;
export function flushPendingLogout() {
  if (!hasPendingLogout()) return Promise.resolve(true);
  if (!flushing)
    flushing = fetch('/api/student/auth/logout', {
      method: 'POST',
      headers: { 'X-UniTrack-Web': '1' },
    })
      .then((response) => {
        if (response.status < 500) {
          clearPendingLogout();
          return true;
        }
        return false;
      })
      .catch(() => false)
      .finally(() => {
        flushing = null;
      });
  return flushing;
}
