import { cookies } from 'next/headers';
import { redirect } from 'next/navigation';
import { ACCESS_COOKIE, REFRESH_COOKIE } from '@/lib/server-session';
import { PortalProvider } from '@/components/portal-provider';
import { PortalShell } from '@/components/portal-shell';

export default async function PortalLayout({ children }: { children: React.ReactNode }) {
  const jar = await cookies();
  if (!jar.has(ACCESS_COOKIE) && !jar.has(REFRESH_COOKIE)) redirect('/login');
  return (
    <>
      <a className="skip-link" href="#main-content">
        İçeriğe geç
      </a>
      <PortalProvider>
        <PortalShell>{children}</PortalShell>
      </PortalProvider>
    </>
  );
}
