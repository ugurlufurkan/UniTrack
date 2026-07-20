import { desc } from "drizzle-orm";

import { db } from "@/lib/db";
import { accountDeletionRequests } from "@/lib/db/schema";

import { markCompleted } from "./actions";

export const dynamic = "force-dynamic";

export default async function DeletionRequestsPage() {
  const items = await db
    .select()
    .from(accountDeletionRequests)
    .orderBy(desc(accountDeletionRequests.createdAt));

  const pending = items.filter((item) => item.status === "pending");
  const completed = items.filter((item) => item.status !== "pending");

  return (
    <div className="space-y-8">
      <div>
        <h1 className="text-lg font-semibold text-slate-900">Hesap Silme Talepleri</h1>
        <p className="text-sm text-slate-500">
          `/legal/delete-account` üzerinden (web) veya uygulama içi Ayarlar → Hesabımı Sil
          üzerinden gelen talepler. Buradan işaretlemek veriyi OTOMATİK silmiyor — backend'de
          ilgili kullanıcıyı bulup verisini silmen/anonimleştirmen, sonra &quot;Tamamlandı&quot;
          işaretlemen gerekiyor.
        </p>
      </div>

      <section className="space-y-3">
        <h2 className="text-sm font-medium text-slate-700">Bekleyen ({pending.length})</h2>
        {pending.length === 0 && <p className="text-sm text-slate-400">Bekleyen talep yok.</p>}
        {pending.map((item) => (
          <div
            key={item.id}
            className="flex items-start justify-between gap-4 rounded-lg border border-amber-200 bg-amber-50 p-4"
          >
            <div>
              <p className="text-sm font-medium text-slate-900">{item.email}</p>
              {item.reason && <p className="mt-1 text-sm text-slate-600">{item.reason}</p>}
              <p className="mt-1 text-xs text-slate-400">
                Talep: {item.createdAt.toLocaleString("tr-TR")}
              </p>
            </div>
            <form action={markCompleted.bind(null, item.id)}>
              <button
                type="submit"
                className="whitespace-nowrap rounded-md border border-slate-300 bg-white px-3 py-1 text-xs hover:bg-slate-100"
              >
                Tamamlandı işaretle
              </button>
            </form>
          </div>
        ))}
      </section>

      <section className="space-y-3">
        <h2 className="text-sm font-medium text-slate-700">Tamamlanan ({completed.length})</h2>
        {completed.map((item) => (
          <div key={item.id} className="rounded-lg border border-slate-200 p-4 text-sm text-slate-500">
            {item.email} — {item.processedAt?.toLocaleString("tr-TR")}
          </div>
        ))}
      </section>
    </div>
  );
}
