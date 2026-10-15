'use client';
import Link from 'next/link';
import Script from 'next/script';
import { useRouter, useSearchParams } from 'next/navigation';
import { useRef, useState } from 'react';
import { api, json } from '@/lib/api';
import { AsyncForm, Brand, Field, Icon } from './ui';

type Mode = 'login' | 'register' | 'forgot-password' | 'reset-password' | 'verify-email';
const copy: Record<Mode, [string, string]> = {
  login: ['Tekrar hoş geldin.', 'Kaldığın yerden devam et. Üniversite hayatın burada.'],
  register: ['Yeni bir başlangıç.', 'Hesabını oluştur, akademik yolculuğunu bir araya getir.'],
  'forgot-password': [
    'Şifreni mi unuttun?',
    'Sıfırlama bağlantısı için hesabının e-posta adresini gir.',
  ],
  'reset-password': ['Yeni şifreni belirle.', 'Güçlü bir şifreyle hesabını güvende tut.'],
  'verify-email': [
    'E-posta adresini doğrula.',
    'Bağlantını onaylayarak hesabının doğrulamasını tamamla.',
  ],
};

function GoogleButton() {
  const ref = useRef<HTMLDivElement>(null);
  const router = useRouter();
  const [error, setError] = useState('');
  const clientId = process.env.NEXT_PUBLIC_GOOGLE_CLIENT_ID;
  if (!clientId) return null;
  const initialize = () => {
    const google = (
      window as unknown as {
        google?: {
          accounts: {
            id: {
              initialize: (options: object) => void;
              renderButton: (target: HTMLElement, options: object) => void;
            };
          };
        };
      }
    ).google;
    if (!google || !ref.current) return;
    google.accounts.id.initialize({
      client_id: clientId,
      callback: async ({ credential }: { credential: string }) => {
        try {
          await api('auth/google', { method: 'POST', body: json({ idToken: credential }) });
          router.replace('/');
          router.refresh();
        } catch (e) {
          setError(e instanceof Error ? e.message : 'Google ile giriş tamamlanamadı.');
        }
      },
    });
    google.accounts.id.renderButton(ref.current, {
      theme: 'outline',
      size: 'large',
      width: 340,
      text: 'continue_with',
      locale: 'tr',
    });
  };
  return (
    <>
      <Script
        src="https://accounts.google.com/gsi/client"
        onReady={initialize}
        onError={() => setError('Google giriş hizmeti yüklenemedi.')}
        strategy="afterInteractive"
      />
      <div className="auth-divider">
        <span>veya</span>
      </div>
      <div ref={ref} className="google-button" />
      {error && (
        <p className="notice error" role="alert">
          {error}
        </p>
      )}
    </>
  );
}

export function AuthScreen({ mode }: { mode: Mode }) {
  const router = useRouter();
  const search = useSearchParams();
  const [done, setDone] = useState('');
  const [showPassword, setShowPassword] = useState(false);
  const needsEmail = ['login', 'register', 'forgot-password'].includes(mode);
  const needsPassword = ['login', 'register', 'reset-password'].includes(mode);
  const token = search.get('token');
  const invalidToken = ['reset-password', 'verify-email'].includes(mode) && !token;
  async function submit(data: FormData) {
    const password = String(data.get('password') || '');
    if (mode === 'register' && password !== data.get('confirmPassword'))
      throw new Error('Şifreler eşleşmiyor.');
    const body =
      mode === 'reset-password'
        ? { token, newPassword: password }
        : mode === 'verify-email'
          ? { token }
          : {
              email: data.get('email'),
              ...(needsPassword ? { password } : {}),
              ...(mode === 'register' ? { name: data.get('name') } : {}),
            };
    const response = await api<{ message?: string }>(`auth/${mode === 'login' ? 'login' : mode}`, {
      method: 'POST',
      body: json(body),
    });
    if (mode === 'login' || mode === 'register') {
      router.replace('/');
      router.refresh();
    } else setDone(response.message || 'İşlem başarıyla tamamlandı.');
  }
  return (
    <main className="auth-page">
      <section className="auth-story">
        <Link href="/login">
          <Brand light />
        </Link>
        <div className="auth-story-content">
          <span className="auth-tag">
            <span />
            Üniversite hayatın, bir arada.
          </span>
          <h1>
            Hedeflerine
            <br />
            bir adım
            <br />
            <em>daha yakın.</em>
          </h1>
          <p>
            Derslerini planla, gelişimini gör, zamanını kendine ayır. UniTrack her adımda yanında.
          </p>
          <div className="auth-preview" aria-label="Örnek portal görünümü">
            <div className="preview-top">
              <span className="preview-dot" />
              <span className="preview-dot" />
              <span className="preview-dot" />
              <span>HAFTALIK PLANIN · ÖRNEK</span>
            </div>
            <div className="preview-item">
              <span className="preview-time">09.00</span>
              <span className="preview-line" />
              <div>
                <strong>Yeni güne bir adım</strong>
                <small>Derslerin ve planların tek yerde</small>
              </div>
              <Icon name="check" size={24} />
            </div>
            <div className="preview-item">
              <span className="preview-time">14.00</span>
              <span className="preview-line teal" />
              <div>
                <strong>Hedefine odaklan</strong>
                <small>Gelişimini her an takip et</small>
              </div>
              <Icon name="chart" size={24} />
            </div>
          </div>
        </div>
        <div className="auth-story-footer">
          <span>Bir hesap. Tüm cihazların.</span>
          <span>WEB + ANDROID ↗</span>
        </div>
      </section>
      <section className="auth-form-side">
        <div className="auth-mobile-brand">
          <Brand />
        </div>
        <div className="auth-form-wrap">
          <span className="eyebrow">SENİN KAMPÜSÜN. SENİN ALANIN.</span>
          <h2>{copy[mode][0]}</h2>
          <p className="auth-description">{copy[mode][1]}</p>
          {search.get('expired') && (
            <p className="notice">Oturumun sona erdi. Devam etmek için tekrar giriş yap.</p>
          )}
          {done ? (
            <div className="notice success" role="status">
              <Icon name="check" />
              {done}
            </div>
          ) : invalidToken ? (
            <p className="notice error">
              Bağlantı eksik veya geçersiz. E-postandaki bağlantıyı yeniden aç.
            </p>
          ) : (
            <AsyncForm
              key={mode}
              onSave={submit}
              label={
                mode === 'login'
                  ? 'Giriş yap →'
                  : mode === 'register'
                    ? 'Hesabımı oluştur →'
                    : mode === 'verify-email'
                      ? 'E-postamı doğrula'
                      : mode === 'forgot-password'
                        ? 'Sıfırlama bağlantısı gönder'
                        : 'Şifremi güncelle'
              }
            >
              {mode === 'register' && (
                <Field label="Ad soyad">
                  <input
                    name="name"
                    autoComplete="name"
                    required
                    minLength={2}
                    maxLength={100}
                    placeholder="Adın ve soyadın"
                  />
                </Field>
              )}
              {needsEmail && (
                <Field label="E-posta adresi">
                  <input
                    name="email"
                    type="email"
                    autoComplete="email"
                    required
                    maxLength={255}
                    placeholder="ornek@universite.edu.tr"
                  />
                </Field>
              )}
              {needsPassword && (
                <>
                  <Field
                    label="Şifre"
                    hint={
                      mode !== 'login'
                        ? 'En az 8 karakter; büyük harf, küçük harf, rakam ve özel karakter kullan.'
                        : undefined
                    }
                  >
                    <div className="password-field">
                      <input
                        name="password"
                        aria-label="Şifre"
                        type={showPassword ? 'text' : 'password'}
                        autoComplete={mode === 'login' ? 'current-password' : 'new-password'}
                        required
                        minLength={mode === 'login' ? 1 : 8}
                        maxLength={72}
                        placeholder="Şifreni gir"
                      />
                      <button
                        type="button"
                        onClick={() => setShowPassword((v) => !v)}
                        aria-label={showPassword ? 'Şifreyi gizle' : 'Şifreyi göster'}
                      >
                        {showPassword ? 'Gizle' : 'Göster'}
                      </button>
                    </div>
                  </Field>
                  {mode === 'login' && (
                    <div className="auth-forgot">
                      <span>
                        <Icon name="shield" size={14} />
                        Güvenli oturum
                      </span>
                      <Link href="/forgot-password">Şifremi unuttum</Link>
                    </div>
                  )}
                </>
              )}
              {mode === 'register' && (
                <>
                  <Field label="Şifre tekrar">
                    <input
                      name="confirmPassword"
                      type="password"
                      autoComplete="new-password"
                      required
                      minLength={8}
                      maxLength={72}
                      placeholder="Şifreni tekrar gir"
                    />
                  </Field>
                  <label className="checkbox-field">
                    <input type="checkbox" required />
                    <span>
                      <Link href="/legal/terms">Kullanım koşullarını</Link> ve{' '}
                      <Link href="/legal/privacy">gizlilik politikasını</Link> okudum.
                    </span>
                  </label>
                </>
              )}
            </AsyncForm>
          )}
          {['login', 'register'].includes(mode) && !done && <GoogleButton />}
          <p className="auth-switch">
            {mode === 'login' ? (
              <>
                Henüz hesabın yok mu? <Link href="/register">Hesap oluştur</Link>
              </>
            ) : (
              <>
                Hesabına dönmek için <Link href="/login">giriş yap</Link>
              </>
            )}
          </p>
          <p className="auth-mobile-note">
            <Icon name="shield" size={16} />
            Mobil uygulamadaki hesabınla giriş yapabilirsin.
          </p>
        </div>
        <footer className="auth-footer">
          <span>© {new Date().getFullYear()} UniTrack</span>
          <Link href="/legal/privacy">Gizlilik politikası</Link>
          <Link href="/legal/delete-account">Hesap silme</Link>
        </footer>
      </section>
    </main>
  );
}
