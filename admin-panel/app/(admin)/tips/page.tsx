import { asc } from "drizzle-orm";

import { db } from "@/lib/db";
import { tips } from "@/lib/db/schema";

import { createTip, deleteTip, updateTip } from "./actions";

export const dynamic = "force-dynamic";

export default async function TipsPage() {
  const items = await db.select().from(tips).orderBy(asc(tips.sortOrder));

  return (
    <div className="space-y-8">
      <div>
        <h1 className="text-lg font-semibold text-slate-900">İpuçları / Motivasyon Mesajları</h1>
        <p className="text-sm text-slate-500">
          Dashboard&apos;da dönüşümlü olarak gösterilir. Sıra numarası küçük olan önce gelir.
        </p>
      </div>

      <form action={createTip} className="space-y-3 rounded-lg border border-slate-200 bg-white p-4">
        <h2 className="text-sm font-medium text-slate-700">Yeni ipucu</h2>
        <textarea
          name="message"
          placeholder="Örn: Sınav haftasına 5 gün kaldı, planını gözden geçir!"
          required
          rows={2}
          className="w-full rounded-md border border-slate-300 px-3 py-2 text-sm text-slate-900"
        />
        <button type="submit" className="rounded-md bg-slate-900 px-3 py-1.5 text-sm text-white hover:bg-slate-800">
          Ekle
        </button>
      </form>

      <div className="space-y-3">
        {items.length === 0 && <p className="text-sm text-slate-400">Henüz ipucu yok.</p>}

        {items.map((item) => (
          <form
            key={item.id}
            action={updateTip.bind(null, item.id)}
            className="space-y-2 rounded-lg border border-slate-200 bg-white p-4"
          >
            <textarea
              name="message"
              defaultValue={item.message}
              rows={2}
              className="w-full rounded-md border border-slate-300 px-3 py-2 text-sm text-slate-900"
            />
            <div className="flex items-center justify-between">
              <div className="flex items-center gap-4">
                <label className="flex items-center gap-2 text-xs text-slate-600">
                  <input type="checkbox" name="isActive" defaultChecked={item.isActive} />
                  Aktif
                </label>
                <label className="flex items-center gap-2 text-xs text-slate-600">
                  Sıra
                  <input
                    type="number"
                    name="sortOrder"
                    defaultValue={item.sortOrder}
                    className="w-16 rounded-md border border-slate-300 px-2 py-1 text-slate-900"
                  />
                </label>
              </div>
              <div className="flex gap-2">
                <button type="submit" className="rounded-md border border-slate-300 px-3 py-1 text-xs hover:bg-slate-100">
                  Kaydet
                </button>
                <button
                  formAction={deleteTip.bind(null, item.id)}
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
