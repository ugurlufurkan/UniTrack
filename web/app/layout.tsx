import type { Metadata } from 'next';
import './globals.css';

export const metadata: Metadata = {
  title: { default: 'UniTrack · Öğrenci Portalı', template: '%s · UniTrack' },
  description: 'Derslerin, notların ve planların tek bir yerde. UniTrack öğrenci portalı.',
};
export default function RootLayout({ children }: { children: React.ReactNode }) {
  return (
    <html lang="tr" data-scroll-behavior="smooth" suppressHydrationWarning>
      <body>{children}</body>
    </html>
  );
}
