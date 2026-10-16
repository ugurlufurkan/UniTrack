import type { Metadata } from 'next';
import './globals.css';
import { PwaRuntime } from '@/components/pwa';

export const metadata: Metadata = {
  title: { default: 'UniTrack · Öğrenci Portalı', template: '%s · UniTrack' },
  description: 'Derslerin, notların ve planların tek bir yerde. UniTrack öğrenci portalı.',
  appleWebApp: { capable: true, title: 'UniTrack', statusBarStyle: 'default' },
  icons: { apple: '/icons/apple-touch-icon.png' },
};
export default function RootLayout({ children }: { children: React.ReactNode }) {
  return (
    <html lang="tr" data-scroll-behavior="smooth" suppressHydrationWarning>
      <body>
        <PwaRuntime />
        {children}
      </body>
    </html>
  );
}
