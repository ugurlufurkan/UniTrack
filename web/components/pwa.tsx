'use client';
import { useEffect, useState } from 'react';
import {
  flushPendingLogout,
  offlineStatus,
  setOfflineEnabled,
  saveOfflineSnapshot,
  pushPermission,
  setPushPermission,
  stopBrowserPush,
} from '@/lib/device-storage';
import { api, json } from '@/lib/api';
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
      <PushPreferences />
    </section>
  );
}

function publicKeyBuffer(value: string): ArrayBuffer {
  const raw = atob(value.replace(/-/g, '+').replace(/_/g, '/'));
  const bytes = new Uint8Array(raw.length);
  for (let n = 0; n < raw.length; n++) bytes[n] = raw.charCodeAt(n);
  return bytes.buffer;
}
function sameKey(subscription: PushSubscription, key: string) {
  const current = subscription.options.applicationServerKey;
  if (!current) return false;
  return (
    btoa(String.fromCharCode(...new Uint8Array(current)))
      .replace(/=/g, '')
      .replace(/\+/g, '-')
      .replace(/\//g, '_') === key
  );
}
function PushPreferences() {
  const { user, notify } = usePortal();
  const [config, setConfig] = useState<{ enabled: boolean; publicKey?: string }>();
  const [enabled, setEnabled] = useState(false);
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState('');
  useEffect(() => {
    let active = true;
    if ('serviceWorker' in navigator && 'PushManager' in window && 'Notification' in window) {
      Promise.all([
        api<{ enabled: boolean; publicKey?: string }>('push/config'),
        pushPermission(),
        navigator.serviceWorker.getRegistration(),
      ])
        .then(async ([settings, state, registration]) => {
          const subscription = await registration?.pushManager.getSubscription();
          if (!active) return;
          setConfig(settings);
          setEnabled(
            !!subscription &&
              state.enabled &&
              state.owner === user.userId &&
              !!settings.publicKey &&
              sameKey(subscription, settings.publicKey),
          );
        })
        .catch(() => {
          if (active) setConfig({ enabled: false });
        });
    }
    return () => {
      active = false;
    };
  }, [user.userId]);
  return (
    <div className="form-stack">
      <h3>Etkinlik hatırlatmaları</h3>
      <p className="muted small-copy">
        İzin verdiğinde takvimindeki bildirim zamanlarında, tarayıcı kapalıyken de hatırlatma
        alabilirsin. Çıkışta bu cihazdaki bildirimler kapanır.
      </p>
      {!config?.enabled && (
        <p className="muted small-copy">
          Hatırlatmalar şu anda bu cihazda kullanılamıyor. iPhone/iPad üzerinde önce ana ekrana
          ekleyip uygulamayı oradan aç.
        </p>
      )}
      <button
        className="button secondary"
        disabled={busy || (!enabled && !config?.enabled)}
        onClick={async () => {
          setBusy(true);
          setError('');
          try {
            if (enabled) {
              await setPushPermission(user.userId, false);
              const registration = await navigator.serviceWorker.getRegistration();
              const subscription = await registration?.pushManager.getSubscription();
              if (subscription)
                await api('push/subscriptions', {
                  method: 'DELETE',
                  body: json({ endpoint: subscription.endpoint }),
                }).catch(() => {});
              await stopBrowserPush();
              setEnabled(false);
              notify('Bu cihazdaki hatırlatmalar kapatıldı.');
            } else {
              if (!config?.publicKey) return;
              if ((await Notification.requestPermission()) !== 'granted')
                throw new Error('Tarayıcı ayarlarından bildirim izni verip tekrar dene.');
              const registration = await navigator.serviceWorker.getRegistration();
              if (!registration?.active)
                throw new Error('Uygulama hazırlanıyor. Sayfayı yenileyip tekrar dene.');
              let subscription = await registration.pushManager.getSubscription();
              if (subscription && !sameKey(subscription, config.publicKey)) {
                await subscription.unsubscribe();
                subscription = null;
              }
              subscription ||= await registration.pushManager.subscribe({
                userVisibleOnly: true,
                applicationServerKey: publicKeyBuffer(config.publicKey),
              });
              await setPushPermission(user.userId, true);
              try {
                await api('push/subscriptions', {
                  method: 'POST',
                  body: json(subscription.toJSON()),
                });
              } catch (e) {
                await setPushPermission(user.userId, false);
                await stopBrowserPush();
                throw e;
              }
              setEnabled(true);
              notify('Bu cihazdaki hatırlatmalar açıldı.');
            }
          } catch (e) {
            setError(e instanceof Error ? e.message : 'Hatırlatmalar güncellenemedi.');
          } finally {
            setBusy(false);
          }
        }}
      >
        {busy ? 'Güncelleniyor…' : enabled ? 'Hatırlatmaları kapat' : 'Hatırlatmaları aç'}
      </button>
      {error && <ErrorNotice error={error} />}
    </div>
  );
}
