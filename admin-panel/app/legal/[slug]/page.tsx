import { eq } from "drizzle-orm";
import type { Metadata } from "next";
import Link from "next/link";
import { notFound } from "next/navigation";

import { db } from "@/lib/db";
import { staticPages } from "@/lib/db/schema";
import { markdownToHtml } from "@/lib/markdown";

// Bu route BİLİNÇLİ olarak `app/(admin)/` grubunun DIŞINDA — yani
// `proxy.ts`'deki auth matcher'ı bunu kapsamıyor ve sayfa girişsiz
// herkese açık. Google Play Console'un "Gizlilik Politikası" alanına
// (App content > Privacy policy) buradaki `/legal/privacy` URL'i
// girilebilir; JSON döndüren `GET /api/v1/content/*` mobil API'sinin
// aksine bu, Play'in istediği gibi doğrudan tarayıcıda okunabilen bir
// HTML sayfası üretir.
export const dynamic = "force-dynamic";

async function getPage(slug: string) {
  const [page] = await db.select().from(staticPages).where(eq(staticPages.slug, slug)).limit(1);
  return page ?? null;
}

export async function generateMetadata({
  params,
}: {
  params: Promise<{ slug: string }>;
}): Promise<Metadata> {
  const { slug } = await params;
  const page = await getPage(slug);
  return { title: page ? `${page.title} — UniTrack` : "Sayfa bulunamadı — UniTrack" };
}

export default async function LegalPage({ params }: { params: Promise<{ slug: string }> }) {
  const { slug } = await params;
  const page = await getPage(slug);

  if (!page) {
    notFound();
  }

  return (
    <div className="min-h-screen bg-white">
      <div className="mx-auto max-w-2xl px-6 py-12">
        <Link href="/legal" className="text-sm text-slate-400 hover:text-slate-600">
          ← Tüm sayfalar
        </Link>
        <h1 className="mt-4 text-3xl font-semibold text-slate-900">{page.title}</h1>
        <p className="mt-1 text-xs text-slate-400">
          Son güncelleme: {page.updatedAt.toLocaleDateString("tr-TR")}
        </p>
        <div
          className="mt-6 text-slate-700"
          dangerouslySetInnerHTML={{ __html: markdownToHtml(page.content) }}
        />
      </div>
    </div>
  );
}
