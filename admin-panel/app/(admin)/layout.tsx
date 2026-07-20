import Link from "next/link";

import { requireAdmin } from "@/lib/require-admin";
import SignOutButton from "./sign-out-button";

const NAV_ITEMS = [
  { href: "/announcements", label: "Duyurular" },
  { href: "/faqs", label: "SSS" },
  { href: "/tips", label: "İpuçları" },
  { href: "/pages", label: "Statik Sayfalar" },
  { href: "/deletion-requests", label: "Hesap Silme Talepleri" },
];

export default async function AdminLayout({ children }: { children: React.ReactNode }) {
  const session = await requireAdmin();

  return (
    <div className="min-h-screen bg-slate-50">
      <header className="border-b border-slate-200 bg-white">
        <div className="mx-auto flex max-w-5xl items-center justify-between px-6 py-4">
          <div className="flex items-center gap-8">
            <span className="text-sm font-semibold text-slate-900">UniTrack Admin</span>
            <nav className="flex gap-5 text-sm text-slate-600">
              {NAV_ITEMS.map((item) => (
                <Link key={item.href} href={item.href} className="hover:text-slate-900">
                  {item.label}
                </Link>
              ))}
            </nav>
          </div>
          <div className="flex items-center gap-3 text-sm text-slate-500">
            <span>{session.user.email}</span>
            <SignOutButton />
          </div>
        </div>
      </header>

      <main className="mx-auto max-w-5xl px-6 py-8">{children}</main>
    </div>
  );
}
