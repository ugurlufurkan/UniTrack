import Link from 'next/link';
import { Brand } from '@/components/ui';
export default function Layout({ children }: { children: React.ReactNode }) {
  return (
    <main className="legal-shell">
      <nav className="legal-nav" aria-label="Genel bağlantılar">
        <Link href="/login">
          <Brand />
        </Link>
        <Link className="button secondary" href="/login">
          Öğrenci portalına git
        </Link>
      </nav>
      {children}
    </main>
  );
}
