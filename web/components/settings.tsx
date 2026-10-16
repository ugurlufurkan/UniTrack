'use client';
import { useState } from 'react';
import { useRouter } from 'next/navigation';
import { api, json } from '@/lib/api';
import type { GradeBand } from '@/lib/types';
import { usePortal, useResource } from './portal-provider';
import { DevicePreferences } from './pwa';
import {
  AsyncForm,
  Badge,
  ConfirmButton,
  Empty,
  ErrorNotice,
  Field,
  Icon,
  Loading,
  Modal,
  PageTitle,
  dateTime,
} from './ui';

type Device = {
  id: string;
  deviceName: string;
  deviceType: string;
  lastUsedAt: string;
  createdAt: string;
};
function Devices() {
  const { data, error, retry } = useResource<{ devices: Device[] }>('auth/devices');
  const { reload, notify } = usePortal();
  const router = useRouter();
  return (
    <section className="panel settings-panel">
      <div className="panel-heading">
        <h2>Açık oturumların</h2>
        <Icon name="shield" />
      </div>
      {error ? (
        <ErrorNotice error={error} retry={retry} />
      ) : !data ? (
        <Loading />
      ) : (
        <>
          {data.devices.map((device) => (
            <div className="device-row" key={device.id}>
              <span className="metric-icon blue">
                <Icon name={device.deviceType === 'mobile' ? 'book' : 'grid'} />
              </span>
              <div>
                <strong>{device.deviceName}</strong>
                <small>Son kullanım: {dateTime(device.lastUsedAt || device.createdAt)}</small>
              </div>
              <ConfirmButton
                className="button secondary compact"
                message="Bu cihazın oturumu kapatılacak ve sonraki API isteğinde yeniden giriş gerekecek."
                onConfirm={async () => {
                  await api(`auth/devices/${device.id}`, { method: 'DELETE' });
                  reload();
                  notify('Cihaz oturumu kapatıldı.');
                }}
              >
                Kapat
              </ConfirmButton>
            </div>
          ))}
          <ConfirmButton
            className="button secondary"
            message="Web ve mobil dahil tüm cihazlardaki yenileme oturumların kapatılacak. Yeniden giriş yapman gerekecek."
            onConfirm={async () => {
              await api('auth/logout-all', { method: 'POST' });
              router.replace('/login');
              router.refresh();
            }}
          >
            Tüm cihazlardan çıkış yap
          </ConfirmButton>
        </>
      )}
    </section>
  );
}

function GradeScaleEditor({ onClose }: { onClose: () => void }) {
  const { scale, reload, notify } = usePortal();
  const [bands, setBands] = useState<GradeBand[]>(scale);
  return (
    <Modal title="Harf notu skalası" onClose={onClose}>
      <AsyncForm
        onSave={async () => {
          await api('grade-scale/default', { method: 'PUT', body: json({ gradeScale: bands }) });
          reload();
          notify('Harf notu skalası kaydedildi.');
          onClose();
        }}
      >
        <p className="notice">
          Bu değişiklik, derse özel skalası olmayan tüm derslerin harf notu ve GANO hesabını
          günceller.
        </p>
        <div className="component-labels">
          <span>Harf</span>
          <span>En az not</span>
          <span>Katsayı</span>
          <span />
        </div>
        {bands.map((band, i) => (
          <div className="component-row" key={i}>
            <input
              aria-label={`${i + 1}. harf`}
              value={band.letter}
              required
              maxLength={4}
              onChange={(e) =>
                setBands((v) =>
                  v.map((b, n) => (n === i ? { ...b, letter: e.target.value.toUpperCase() } : b)),
                )
              }
            />
            <input
              aria-label={`${band.letter} minimum not`}
              type="number"
              min={0}
              max={100}
              required
              value={band.min}
              onChange={(e) =>
                setBands((v) =>
                  v.map((b, n) => (n === i ? { ...b, min: Number(e.target.value) } : b)),
                )
              }
            />
            <input
              aria-label={`${band.letter} katsayısı`}
              type="number"
              min={0}
              max={4.5}
              step="0.01"
              required
              value={band.point}
              onChange={(e) =>
                setBands((v) =>
                  v.map((b, n) => (n === i ? { ...b, point: Number(e.target.value) } : b)),
                )
              }
            />
            <button
              type="button"
              className="icon-button danger"
              disabled={bands.length <= 2}
              aria-label={`${band.letter} aralığını kaldır`}
              onClick={() => setBands((v) => v.filter((_, n) => n !== i))}
            >
              <Icon name="close" size={16} />
            </button>
          </div>
        ))}
        <button
          type="button"
          className="text-button"
          disabled={bands.length >= 20}
          onClick={() => setBands((v) => [...v, { letter: '', min: 0, point: 0 }])}
        >
          <Icon name="plus" size={16} />
          Aralık ekle
        </button>
      </AsyncForm>
      <div className="modal-extra">
        <ConfirmButton
          className="text-button"
          message="Harf notu skalan sistem varsayılanına dönecek ve ilgili derslerin notları yeniden hesaplanacak."
          onConfirm={async () => {
            await api('grade-scale/default', { method: 'PUT', body: json({ gradeScale: null }) });
            reload();
            notify('Varsayılan skala geri yüklendi.');
            onClose();
          }}
        >
          Sistem varsayılanına dön
        </ConfirmButton>
      </div>
    </Modal>
  );
}

export function Settings() {
  const { user, settings, reload, notify } = usePortal();
  const router = useRouter();
  const [scaleOpen, setScaleOpen] = useState(false);
  const [deleting, setDeleting] = useState(false);
  const [exportError, setExportError] = useState('');
  const [exportBusy, setExportBusy] = useState(false);
  async function download() {
    setExportBusy(true);
    setExportError('');
    try {
      const data = await api('export');
      const url = URL.createObjectURL(
        new Blob([JSON.stringify(data, null, 2)], { type: 'application/json' }),
      );
      const anchor = document.createElement('a');
      anchor.href = url;
      anchor.download = `unitrack-yedek-${new Date().toISOString().slice(0, 10)}.json`;
      anchor.click();
      setTimeout(() => URL.revokeObjectURL(url), 1000);
      notify('Hesap verilerin indirildi.');
    } catch (e) {
      setExportError(e instanceof Error ? e.message : 'İndirme başarısız.');
    } finally {
      setExportBusy(false);
    }
  }
  return (
    <>
      <PageTitle
        eyebrow="SANA GÖRE"
        title="Hesap ve tercihler"
        description="Hedeflerini, görünümünü ve hesap güvenliğini yönet."
      />
      <div className="settings-grid">
        <section className="panel settings-panel">
          <div className="panel-heading">
            <h2>Akademik tercihler</h2>
            <Icon name="target" />
          </div>
          <AsyncForm
            key={`${settings.targetGpa}-${settings.themePreference}-${settings.examPeriodStart}-${settings.examPeriodEnd}`}
            onSave={async (data) => {
              const start = String(data.get('start') || '');
              const end = String(data.get('end') || '');
              if (start && end && start > end)
                throw new Error('Sınav dönemi başlangıcı bitişten sonra olamaz.');
              await api('settings', {
                method: 'PATCH',
                body: json({
                  targetGpa: data.get('targetGpa') === '' ? null : Number(data.get('targetGpa')),
                  themePreference: data.get('theme'),
                  examPeriodStart: start || null,
                  examPeriodEnd: end || null,
                }),
              });
              reload();
              notify('Tercihlerin kaydedildi.');
            }}
          >
            <Field label="Hedef GANO" hint="Boş bırakırsan hedefin sıfırlanır.">
              <input
                name="targetGpa"
                type="number"
                min={0}
                max={4.5}
                step="0.01"
                placeholder="Örn. 3,50"
                defaultValue={settings.targetGpa ?? ''}
              />
            </Field>
            <Field label="Görünüm">
              <select name="theme" defaultValue={settings.themePreference || 'SYSTEM'}>
                <option value="SYSTEM">Sistem ayarı</option>
                <option value="LIGHT">Açık tema</option>
                <option value="DARK">Koyu tema</option>
              </select>
            </Field>
            <div className="form-grid">
              <Field label="Sınav dönemi başlangıcı">
                <input
                  name="start"
                  type="date"
                  defaultValue={settings.examPeriodStart?.slice(0, 10) || ''}
                />
              </Field>
              <Field label="Sınav dönemi bitişi">
                <input
                  name="end"
                  type="date"
                  defaultValue={settings.examPeriodEnd?.slice(0, 10) || ''}
                />
              </Field>
            </div>
          </AsyncForm>
          <div className="settings-divider">
            <div>
              <h3>Harf notu skalası</h3>
              <p>Üniversitene uygun not aralıklarını belirle.</p>
            </div>
            <button className="button secondary" onClick={() => setScaleOpen(true)}>
              Düzenle
            </button>
          </div>
        </section>
        <section className="panel settings-panel">
          <div className="panel-heading">
            <h2>Hesap güvenliği</h2>
            <Icon name="shield" />
          </div>
          <div className="profile-summary">
            <span className="avatar">{(user.name || user.email)[0].toUpperCase()}</span>
            <div>
              <strong>{user.name || 'Öğrenci hesabı'}</strong>
              <p>{user.email}</p>
            </div>
            <Badge tone={user.isEmailVerified ? 'green' : 'amber'}>
              {user.isEmailVerified ? 'Doğrulandı' : 'Doğrulanmadı'}
            </Badge>
          </div>
          {!user.isEmailVerified && (
            <AsyncForm
              label="Doğrulama e-postası gönder"
              onSave={async () => {
                await api('auth/verify-email/request', { method: 'POST' });
                notify('Doğrulama bağlantısı gönderildi.');
              }}
            >
              <p className="muted">E-posta adresini doğrulayarak hesabını tamamla.</p>
            </AsyncForm>
          )}
          <h3 className="settings-subtitle">Şifreni değiştir</h3>
          <AsyncForm
            label="Şifremi değiştir"
            onSave={async (form) => {
              if (form.get('newPassword') !== form.get('confirmation'))
                throw new Error('Yeni şifreler eşleşmiyor.');
              await api('auth/change-password', {
                method: 'POST',
                body: json({
                  currentPassword: form.get('currentPassword'),
                  newPassword: form.get('newPassword'),
                }),
              });
              router.replace('/login');
              router.refresh();
            }}
          >
            <Field label="Mevcut şifre">
              <input
                name="currentPassword"
                type="password"
                required
                maxLength={72}
                autoComplete="current-password"
              />
            </Field>
            <Field
              label="Yeni şifre"
              hint="En az 8 karakter; büyük harf, küçük harf, rakam ve özel karakter kullan."
            >
              <input
                name="newPassword"
                type="password"
                minLength={8}
                maxLength={72}
                required
                autoComplete="new-password"
              />
            </Field>
            <Field label="Yeni şifre tekrar">
              <input
                name="confirmation"
                type="password"
                required
                minLength={8}
                maxLength={72}
                autoComplete="new-password"
              />
            </Field>
          </AsyncForm>
        </section>
        <Devices />
        <DevicePreferences />
        <section className="panel settings-panel">
          <div className="panel-heading">
            <h2>Verilerin senin kontrolünde</h2>
            <Icon name="file" />
          </div>
          <p className="muted">Hesap verilerinin bir kopyasını JSON olarak indirebilirsin.</p>
          <button className="button secondary" onClick={download} disabled={exportBusy}>
            <Icon name="download" size={17} />
            {exportBusy ? 'Hazırlanıyor…' : 'Verilerimi indir'}
          </button>
          {exportError && <ErrorNotice error={exportError} />}
          <div className="danger-zone">
            <h3>Hesabımı sil</h3>
            <p>
              Hesabın ve ilişkili ders, not, devamsızlık, görev ve takvim verilerin kalıcı olarak
              silinir.
            </p>
            <button className="button danger-button" onClick={() => setDeleting(true)}>
              Hesabımı kalıcı olarak sil
            </button>
          </div>
        </section>
      </div>
      {scaleOpen && <GradeScaleEditor onClose={() => setScaleOpen(false)} />}
      {deleting && (
        <Modal title="Hesabını kalıcı olarak sil" onClose={() => setDeleting(false)}>
          <AsyncForm
            label="Hesabımı ve verilerimi sil"
            onSave={async (form) => {
              if (String(form.get('email')).trim().toLowerCase() !== user.email.toLowerCase())
                throw new Error('E-posta adresi hesabınla eşleşmiyor.');
              await api('auth/me', { method: 'DELETE' });
              router.replace('/login');
              router.refresh();
            }}
          >
            <p className="notice error">
              Bu işlem geri alınamaz. Devam etmeden önce istersen verilerini indir.
            </p>
            <Field label="Onaylamak için hesap e-postanı yaz">
              <input
                type="email"
                name="email"
                required
                autoComplete="off"
                placeholder={user.email}
              />
            </Field>
          </AsyncForm>
        </Modal>
      )}
    </>
  );
}

export function Help() {
  const announcements =
    useResource<{ id: string; title: string; body: string; createdAt: string }[]>(
      'content/announcements',
    );
  const faqs = useResource<{ id: string; question: string; answer: string }[]>('content/faqs');
  return (
    <>
      <PageTitle
        eyebrow="KAMPÜSTEN HABERLER"
        title="Duyurular ve yardım"
        description="Güncellemeleri takip et, aklına takılan sorulara yanıt bul."
      />
      <div className="insights-grid">
        <section className="panel">
          <div className="panel-heading">
            <h2>Duyurular</h2>
            <Icon name="bell" />
          </div>
          {announcements.error ? (
            <ErrorNotice error={announcements.error} retry={announcements.retry} />
          ) : !announcements.data ? (
            <Loading />
          ) : !announcements.data.length ? (
            <Empty icon="bell" title="Şimdilik yeni bir haber yok.">
              Yayınlanan duyurular burada listelenir.
            </Empty>
          ) : (
            announcements.data.map((item) => (
              <article key={item.id} className="announcement">
                <span className="section-kicker">
                  {new Date(item.createdAt).toLocaleDateString('tr-TR')}
                </span>
                <h3>{item.title}</h3>
                <p>{item.body}</p>
              </article>
            ))
          )}
        </section>
        <section className="panel">
          <div className="panel-heading">
            <h2>Sıkça sorulan sorular</h2>
            <Icon name="book" />
          </div>
          {faqs.error ? (
            <ErrorNotice error={faqs.error} retry={faqs.retry} />
          ) : !faqs.data ? (
            <Loading />
          ) : !faqs.data.length ? (
            <Empty title="Sorular için alanımız hazır.">
              Yayınlanan yardım içeriklerini burada bulacaksın.
            </Empty>
          ) : (
            faqs.data.map((item) => (
              <details className="faq-item" key={item.id}>
                <summary>{item.question}</summary>
                <p>{item.answer}</p>
              </details>
            ))
          )}
        </section>
      </div>
    </>
  );
}
