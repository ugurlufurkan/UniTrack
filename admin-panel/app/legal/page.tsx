import { asc } from "drizzle-orm";
import Link from "next/link";

import { db } from "@/lib/db";
import { staticPages } from "@/lib/db/schema";

export const dynamic = "force-dynamic";

export const metadata = {
  title: "Yasal Bilgiler — UniTrack",
};

export default async function LegalIndexPage() {
  const items = await db.select().from(staticPages).orderBy(asc(staticPages.slug));

  return (
    <div className="min-h-screen bg-white">
      <div className="mx-auto max-w-2xl px-6 py-12">
        <h1 className="text-2xl font-semibold text-slate-900">Yasal Bilgiler</h1>
        <ul className="mt-6 space-y-2">
          {items.map((item) => (
            <li key={item.slug}>
              <Link href={`/legal/${item.slug}`} className="text-blue-600 underline hover:text-blue-800">
                {item.title}
              </Link>
            </li>
          ))}
          <li>
            <Link href="/legal/delete-account" className="text-blue-600 underline hover:text-blue-800">
              Hesap Silme Talebi
            </Link>
          </li>
        </ul>
      </div>
    </div>
  );
}
