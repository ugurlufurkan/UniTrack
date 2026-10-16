'use client';
import { useEffect, useState } from 'react';
import {
  flushPendingLogout,
  offlineStatus,
  setOfflineEnabled,
  saveOfflineSnapshot,
} from '@/lib/device-storage';
import { usePortal } from './portal-provider';
import { ErrorNotice } from './ui';

type InstallEvent = Event & { prompt: () => Promise<void> };
let installPrompt: InstallEvent | null = null;
export function PwaRuntime() {
  useEffect(() => {
    if ('serviceWorker' in navigator && process.env.NODE_ENV === 'production')
      navigator.serviceWorker.register('/sw.js').catch(() => {});
    const ready = (event: Event) => {
      event.preventDefault();
      installPrompt = event as InstallEvent;
    };
    const online = () => {
      void flushPendingLogout();
    };
    window.addEventListener('beforeinstallprompt', ready);
    window.addEventListener('online', online);
    online();
    return () => {
      window.removeEventListener('beforeinstallprompt', ready);
      window.removeEventListener('online', online);
    };
  }, []);
  return null;
}

export function DevicePreferences() {
  const { user, courses, semesters, notify } = usePortal();
  const [enabled, setEnabled] = useState(false);
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState('');
  useEffect(() => {
    offlineStatus()
      .then((status) => setEnabled(status.enabled))
      .catch(() => {});
  }, []);
  return (
    <section className="panel settings-panel">
      <div className="panel-heading">
        <h2>Bu cihazda UniTrack</h2>
      </div>
      <p className="muted">
        Uygulamayı ana ekranına ekleyebilir, derslerinin son kopyasını bağlantın olmadığında
        okuyabilirsin.
      </p>
      <button
        className="button secondary"
        onClick={async () => {
          if (installPrompt) {
            await installPrompt.prompt();
            installPrompt = null;
          } else
            notify(
              'Tarayıcı menüsündeki “Uygulamayı yükle” veya “Ana ekrana ekle” seçeneğini kullan.',
            );
        }}
      >
        Ana ekrana ekle
      </button>
      <label className="checkbox-field">
        <input
          type="checkbox"
          checked={enabled}
          disabled={busy}
          onChange={async (event) => {
            const next = event.target.checked;
            setEnabled(next);
            setBusy(true);
            setError('');
            try {
              await setOfflineEnabled(next);
              if (next) await saveOfflineSnapshot(user, semesters, courses);
              setEnabled(next);
              notify(next ? 'Çevrimdışı ders kopyası hazır.' : 'Cihazdaki ders kopyası silindi.');
            } catch (e) {
              setEnabled(!next);
              await setOfflineEnabled(!next).catch(() => {});
              setError(e instanceof Error ? e.message : 'Kopya kaydedilemedi.');
            } finally {
              setBusy(false);
            }
          }}
        />
        Çevrimdışı ders özeti sakla
      </label>
      <p className="muted small-copy">
        Bu cihazı kullanan kişiler kopyayı okuyabilir; paylaşılan cihazlarda açma. Kopya salt
        okunurdur, 7 gün geçerlidir ve çıkışta temizlenir. Şifre ve oturum anahtarları saklanmaz.
      </p>
      {error && <ErrorNotice error={error} />}
    </section>
  );
}
