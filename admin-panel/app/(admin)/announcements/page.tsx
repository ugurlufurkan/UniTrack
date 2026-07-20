import { desc } from "drizzle-orm";

import { db } from "@/lib/db";
import { announcements } from "@/lib/db/schema";

import { createAnnouncement, deleteAnnouncement, updateAnnouncement } from "./actions";

export const dynamic = "force-dynamic";

export default async function AnnouncementsPage() {
  const items = await db.select().from(announcements).orderBy(desc(announcements.createdAt));

  return (
    <div className="space-y-8">
      <div>
        <h1 className="text-lg font-semibold text-slate-900">Duyurular</h1>
        <p className="text-sm text-slate-500">
          Burada eklediğiniz duyurular, aktifse tüm kullanıcılara mobil uygulamada gösterilir.
        </p>
      </div>

      <form
        action={createAnnouncement}
        className="space-y-3 rounded-lg border border-slate-200 bg-white p-4"
      >
        <h2 className="text-sm font-medium text-slate-700">Yeni duyuru</h2>
        <input
          name="title"
          placeholder="Başlık"
          required
          className="w-full rounded-md border border-slate-300 px-3 py-2 text-sm text-slate-900"
        />
        <textarea
          name="body"
          placeholder="İçerik"
          required
          rows={3}
          className="w-full rounded-md border border-slate-300 px-3 py-2 text-sm text-slate-900"
        />
        <button
          type="submit"
          className="rounded-md bg-slate-900 px-3 py-1.5 text-sm text-white hover:bg-slate-800"
        >
          Ekle
        </button>
      </form>

      <div className="space-y-3">
        {items.length === 0 && (
          <p className="text-sm text-slate-400">Henüz duyuru yok.</p>
        )}

        {items.map((item) => (
          <form
            key={item.id}
            action={updateAnnouncement.bind(null, item.id)}
            className="space-y-2 rounded-lg border border-slate-200 bg-white p-4"
          >
            <input
              name="title"
              defaultValue={item.title}
              className="w-full rounded-md border border-slate-300 px-3 py-2 text-sm font-medium text-slate-900"
            />
            <textarea
              name="body"
              defaultValue={item.body}
              rows={2}
              className="w-full rounded-md border border-slate-300 px-3 py-2 text-sm text-slate-900"
            />
            <div className="flex items-center justify-between">
              <label className="flex items-center gap-2 text-xs text-slate-600">
                <input type="checkbox" name="isActive" defaultChecked={item.isActive} />
                Aktif (uygulamada görünsün)
              </label>
              <div className="flex gap-2">
                <button
                  type="submit"
                  className="rounded-md border border-slate-300 px-3 py-1 text-xs hover:bg-slate-100"
                >
                  Kaydet
                </button>
                <button
                  formAction={deleteAnnouncement.bind(null, item.id)}
                  className="rounded-md border border-red-200 px-3 py-1 text-xs text-red-600 hover:bg-red-50"
                >
                  Sil
                </button>
              </div>
            </div>
          </form>
        ))}
      </div>
    </div>
  );
}
