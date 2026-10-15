'use client';
import Link from 'next/link';
import { usePathname, useRouter } from 'next/navigation';
import { useState, type ReactNode } from 'react';
import { api } from '@/lib/api';
import { semesterLabel } from '@/lib/academic.mjs';
import { usePortal } from './portal-provider';
import { Brand, Icon } from './ui';

const navigation = [
  ['/', 'grid', 'Genel bakış'],
  ['/courses', 'book', 'Dersler ve notlar'],
  ['/semesters', 'cap', 'Dönemlerim'],
  ['/schedule', 'calendar', 'Ders programı'],
  ['/attendance', 'clock', 'Devamsızlık'],
  ['/tasks', 'check', 'Görevlerim'],
  ['/calendar', 'calendar', 'Akademik takvim'],
  ['/gpa', 'target', 'GANO simülatörü'],
  ['/statistics', 'chart', 'İstatistikler'],
  ['/transcript', 'file', 'Transkript'],
];

export function PortalShell({ children }: { children: ReactNode }) {
  const path = usePathname();
  const router = useRouter();
  const { user, semesters, semesterId, setSemesterId } = usePortal();
  const [open, setOpen] = useState(false);
  const [leaving, setLeaving] = useState(false);
  const current =
    navigation.find((n) => n[0] === path)?.[2] ||
    (path === '/settings' ? 'Ayarlar' : 'Duyurular ve yardım');
  const name = user.name || user.email.split('@')[0];
  const initials = name
    .split(' ')
    .map((s) => s[0])
    .slice(0, 2)
    .join('')
    .toLocaleUpperCase('tr-TR');
  async function logout() {
    setLeaving(true);
    try {
      await api('auth/logout', { method: 'POST' });
    } catch {
      // The gateway clears browser cookies even if the upstream logout fails.
    } finally {
      router.replace('/login');
      router.refresh();
    }
  }
  return (
    <div className="portal-shell">
      {open && (
        <button
          className="sidebar-overlay"
          aria-label="Menüyü kapat"
          onClick={() => setOpen(false)}
        />
      )}
      <aside className={`sidebar ${open ? 'is-open' : ''}`}>
        <Link href="/" className="sidebar-brand" onClick={() => setOpen(false)}>
          <Brand />
        </Link>
        <div className="sidebar-section-label">ÇALIŞMA ALANIM</div>
        <nav aria-label="Ana menü">
          {navigation.map(([href, icon, label]) => (
            <Link
              key={href}
              href={href}
              aria-current={path === href ? 'page' : undefined}
              className={`nav-link ${path === href ? 'active' : ''}`}
              onClick={() => setOpen(false)}
            >
              <Icon name={icon} />
              <span>{label}</span>
              {path === href && <span className="nav-dot" />}
            </Link>
          ))}
        </nav>
        <div className="sidebar-bottom">
          <div className="sidebar-note">
            <span className="note-spark">✦</span>
            <strong>Küçük adımlar, büyük hedefler.</strong>
            <p>Bugünün planını yap, yarınına bir adım yaklaş.</p>
          </div>
          <Link
            href="/help"
            className={`nav-link ${path === '/help' ? 'active' : ''}`}
            onClick={() => setOpen(false)}
          >
            <Icon name="bell" />
            Duyurular ve yardım
          </Link>
          <Link
            href="/settings"
            className={`nav-link ${path === '/settings' ? 'active' : ''}`}
            onClick={() => setOpen(false)}
          >
            <Icon name="settings" />
            Ayarlar
          </Link>
          <div className="sidebar-user">
            <span className="avatar">{initials}</span>
            <span className="user-info">
              <strong>{name}</strong>
              <small>Öğrenci hesabı</small>
            </span>
            <button
              className="icon-button"
              onClick={logout}
              disabled={leaving}
              aria-label="Çıkış yap"
            >
              <Icon name="logout" size={18} />
            </button>
          </div>
        </div>
      </aside>
      <div className="workspace">
        <header className="topbar">
          <div className="breadcrumb">
            <button
              className="icon-button menu-toggle"
              aria-expanded={open}
              aria-label="Menüyü aç"
              onClick={() => setOpen(true)}
            >
              <Icon name="menu" />
            </button>
            <span>Öğrenci portalı</span>
            <span className="breadcrumb-divider">/</span>
            <strong>{current}</strong>
          </div>
          <div className="topbar-actions">
            <span className="sync-label">
              <i />
              Hesabınla senkronize
            </span>
            <Link href="/help" className="icon-button" aria-label="Duyurular">
              <Icon name="bell" />
            </Link>
            <span className="avatar small">{initials}</span>
          </div>
        </header>
        <div className="workspace-toolbar">
          <span className="workspace-date">
            {new Date().toLocaleDateString('tr-TR', {
              day: 'numeric',
              month: 'long',
              year: 'numeric',
              weekday: 'long',
            })}
          </span>
          <label className="semester-picker">
            <Icon name="cap" size={17} />
            <select
              aria-label="Aktif dönem"
              value={semesterId}
              onChange={(e) => setSemesterId(e.target.value)}
              disabled={!semesters.length}
            >
              {semesters.length ? (
                semesters.map((s) => (
                  <option key={s.id} value={s.id}>
                    {semesterLabel(s)}
                  </option>
                ))
              ) : (
                <option value="">Henüz dönem yok</option>
              )}
            </select>
          </label>
        </div>
        <main id="main-content" className="page-content">
          {children}
        </main>
        <footer className="workspace-footer">
          <span>UniTrack · Üniversite hayatın, bir arada.</span>
          <Link href="/legal/privacy">Gizlilik</Link>
        </footer>
      </div>
    </div>
  );
}
