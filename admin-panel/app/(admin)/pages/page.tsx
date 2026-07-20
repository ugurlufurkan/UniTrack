import { asc } from "drizzle-orm";

import { db } from "@/lib/db";
import { staticPages } from "@/lib/db/schema";

import { createPage, deletePage, updatePage } from "./actions";

export const dynamic = "force-dynamic";

export default async function StaticPagesPage() {
  const items = await db.select().from(staticPages).orderBy(asc(staticPages.slug));

  return (
    <div className="space-y-8">
      <div>
        <h1 className="text-lg font-semibold text-slate-900">Statik Sayfalar</h1>
        <p className="text-sm text-slate-500">
          Hakkımızda, Gizlilik Politikası, KVKK Aydınlatma Metni gibi sayfalar. İçerik Markdown
          olarak yazılabilir. Mobil uygulama bu sayfaları <code>slug</code> ile çekiyor — slug
          oluşturulduktan sonra değiştirilemez. Her sayfa ayrıca{" "}
          <code>/legal/&#123;slug&#125;</code> adresinde girişsiz, herkese açık olarak da
          görünür — Google Play Console&apos;a Gizlilik Politikası URL&apos;i olarak bu adresi
          (örn. <code>/legal/privacy</code>) verebilirsin.
        </p>
      </div>

      <form action={createPage} className="space-y-3 rounded-lg border border-slate-200 bg-white p-4">
        <h2 className="text-sm font-medium text-slate-700">Yeni sayfa</h2>
        <input
          name="slug"
          placeholder="slug (örn: privacy, about, terms) — sadece küçük harf, rakam, tire"
          required
          pattern="[a-z0-9-]+"
          className="w-full rounded-md border border-slate-300 px-3 py-2 text-sm font-mono"
        />
        <input
          name="title"
          placeholder="Başlık"
          required
          className="w-full rounded-md border border-slate-300 px-3 py-2 text-sm"
        />
        <textarea
          name="content"
          placeholder="İçerik (Markdown)"
          required
          rows={6}
          className="w-full rounded-md border border-slate-300 px-3 py-2 text-sm font-mono"
        />
        <button type="submit" className="rounded-md bg-slate-900 px-3 py-1.5 text-sm text-white hover:bg-slate-800">
          Oluştur
        </button>
      </form>

      <div className="space-y-3">
        {items.length === 0 && <p className="text-sm text-slate-400">Henüz sayfa yok.</p>}

        {items.map((item) => (
          <form
            key={item.id}
            action={updatePage.bind(null, item.id)}
            className="space-y-2 rounded-lg border border-slate-200 bg-white p-4"
          >
            <div className="flex items-center gap-2">
              <span className="rounded bg-slate-100 px-2 py-0.5 font-mono text-xs text-slate-500">
                /{item.slug}
              </span>
              <span className="text-xs text-slate-400">
                Son güncelleme: {item.updatedAt.toLocaleString("tr-TR")}
              </span>
            </div>
            <input
              name="title"
              defaultValue={item.title}
              className="w-full rounded-md border border-slate-300 px-3 py-2 text-sm font-medium"
            />
            <textarea
              name="content"
              defaultValue={item.content}
              rows={6}
              className="w-full rounded-md border border-slate-300 px-3 py-2 text-sm font-mono"
            />
            <div className="flex justify-end gap-2">
              <button type="submit" className="rounded-md border border-slate-300 px-3 py-1 text-xs hover:bg-slate-100">
                Kaydet
              </button>
              <button
                formAction={deletePage.bind(null, item.id)}
                className="rounded-md border border-red-200 px-3 py-1 text-xs text-red-600 hover:bg-red-50"
              >
                Sil
              </button>
            </div>
          </form>
        ))}
      </div>
    </div>
  );
}
