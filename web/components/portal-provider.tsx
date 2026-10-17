'use client';
import { createContext, useCallback, useContext, useEffect, useState, type ReactNode } from 'react';
import { usePathname, useRouter } from 'next/navigation';
import { api, ApiError, SESSION_EXPIRED_EVENT } from '@/lib/api';
import { sortSemesters } from '@/lib/academic.mjs';
import type { Course, GradeBand, Semester, Settings, User } from '@/lib/types';
import { ErrorNotice, Loading } from './ui';
import {
  saveOfflineSnapshot,
  clearDeviceData,
  SESSION_REVISION_KEY,
  hasPendingLogout,
} from '@/lib/device-storage';

type Core = {
  user: User;
  courses: Course[];
  semesters: Semester[];
  settings: Settings;
  scale: GradeBand[];
};
type Context = Core & {
  semesterId: string;
  setSemesterId: (id: string) => void;
  reload: () => void;
  notify: (message: string) => void;
  revision: number;
};
const PortalContext = createContext<Context | null>(null);
export function usePortal() {
  const value = useContext(PortalContext);
  if (!value) throw new Error('Portal provider missing');
  return value;
}

export function useResource<T>(path: string) {
  const { revision } = usePortal();
  const [attempt, setAttempt] = useState(0);
  const [state, setState] = useState<{ data?: T; error?: string }>({});
  useEffect(() => {
    const controller = new AbortController();
    api<T>(path, { signal: controller.signal })
      .then((data) => {
        if (!controller.signal.aborted) setState({ data });
      })
      .catch((error) => {
        if (!controller.signal.aborted) setState({ error: error.message });
      });
    return () => controller.abort();
  }, [path, revision, attempt]);
  return {
    ...state,
    retry: () => {
      setState({});
      setAttempt((a) => a + 1);
    },
  };
}

export function PortalProvider({ children }: { children: ReactNode }) {
  const router = useRouter();
  const pathname = usePathname();
  const [data, setData] = useState<Core | null>(null);
  const [error, setError] = useState('');
  const [revision, setRevision] = useState(0);
  const [selected, setSelected] = useState('');
  const [message, setMessage] = useState('');
  const reload = useCallback(() => setRevision((v) => v + 1), []);
  const notify = useCallback((value: string) => setMessage(value), []);
  useEffect(() => {
    const changed = () => {
      setData(null);
      setSelected('');
      setError('');
      if (hasPendingLogout()) router.replace('/login');
      reload();
    };
    const storage = (event: StorageEvent) => {
      if (event.key === SESSION_REVISION_KEY) changed();
    };
    window.addEventListener('storage', storage);
    return () => {
      window.removeEventListener('storage', storage);
    };
  }, [reload, router]);
  useEffect(() => {
    const expired = () => {
      void clearDeviceData();
      router.replace('/login?expired=1');
    };
    window.addEventListener(SESSION_EXPIRED_EVENT, expired);
    return () => window.removeEventListener(SESSION_EXPIRED_EVENT, expired);
  }, [router]);
  useEffect(() => {
    let active = true;
    Promise.all([
      api<{ user: User }>('auth/me'),
      api<Semester[]>('semesters'),
      api<Course[]>('courses'),
      api<Settings>('settings'),
      api<{ gradeScale: GradeBand[] }>('grade-scale/default'),
    ])
      .then(([profile, semesters, courses, settings, scale]) => {
        if (active) {
          setData({
            user: profile.user,
            semesters: sortSemesters(semesters),
            courses,
            settings,
            scale: scale.gradeScale,
          });
          setError('');
          void saveOfflineSnapshot(profile.user, semesters, courses).catch(() => {});
        }
      })
      .catch((e) => {
        if (active) {
          if (e instanceof ApiError && e.status === 401) {
            void clearDeviceData();
            router.replace('/login?expired=1');
          } else setError(e.message);
        }
      });
    return () => {
      active = false;
    };
  }, [revision, router, pathname]);
  useEffect(() => {
    let lastFocus = 0;
    const sync = () => {
      if (document.visibilityState === 'visible' && Date.now() - lastFocus > 15000) {
        lastFocus = Date.now();
        reload();
      }
    };
    window.addEventListener('focus', sync);
    document.addEventListener('visibilitychange', sync);
    return () => {
      window.removeEventListener('focus', sync);
      document.removeEventListener('visibilitychange', sync);
    };
  }, [reload]);
  useEffect(() => {
    if (!message) return;
    const timer = setTimeout(() => setMessage(''), 4500);
    return () => clearTimeout(timer);
  }, [message]);
  useEffect(() => {
    const preference = data?.settings.themePreference;
    const media = window.matchMedia('(prefers-color-scheme: dark)');
    const update = () => {
      document.documentElement.dataset.theme =
        preference === 'DARK' || (preference === 'SYSTEM' && media.matches) ? 'dark' : 'light';
    };
    update();
    media.addEventListener('change', update);
    return () => media.removeEventListener('change', update);
  }, [data?.settings.themePreference]);
  if (!data)
    return (
      <div className="standalone">
        {error ? <ErrorNotice error={error} retry={reload} /> : <Loading />}
      </div>
    );
  const semesterId = data.semesters.some((s) => s.id === selected)
    ? selected
    : data.semesters[0]?.id || '';
  return (
    <PortalContext.Provider
      value={{ ...data, semesterId, setSemesterId: setSelected, reload, notify, revision }}
    >
      {error && (
        <div className="sync-error">
          <ErrorNotice error={error} retry={reload} />
        </div>
      )}
      {children}
      {message && (
        <div className="toast" role="status">
          <IconCheck />
          {message}
        </div>
      )}
    </PortalContext.Provider>
  );
}
function IconCheck() {
  return <span aria-hidden="true">✓</span>;
}
