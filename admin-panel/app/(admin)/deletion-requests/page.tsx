import { desc } from "drizzle-orm";

import { db } from "@/lib/db";
import { accountDeletionRequests } from "@/lib/db/schema";

import { deleteVerifiedRequest } from "./actions";

export const dynamic = "force-dynamic";

export default async function DeletionRequestsPage() {
  const items = await db
    .select()
    .from(accountDeletionRequests)
    .orderBy(desc(accountDeletionRequests.createdAt));

  // Doğrulama bekleyen: kişi henüz e-postasındaki onay linkine tıklamamış.
  // Burada GERÇEK bir işlem yapılamaz — sadece bilgi amaçlı.
  const awaitingVerification = items.filter(
    (item) => item.status === "pending" && !item.verifiedAt
  );
  // Doğrulanmış: e-postanın sahibi olduğu kanıtlandı, silinmeyi bekliyor.
  const verifiedPending = items.filter(
    (item) => item.status === "pending" && item.verifiedAt
  );
  const completed = items.filter((item) => item.status !== "pending");

  return (
    <div className="space-y-8">
      <div>
        <h1 className="text-lg font-semibold text-slate-900">Hesap Silme Talepleri</h1>
        <p className="text-sm text-slate-500">
          <code>/legal/delete-account</code> üzerinden (web) veya uygulama içi
          Ayarlar → Hesabımı Sil üzerinden gelen talepler. E-posta onayı
          alınmadan hiçbir veri silinmez — &quot;Kalıcı Olarak Sil&quot; butonu
          sadece talep sahibinin e-postasındaki onay linkine tıklamasından
          sonra aktif olur, ve tıklandığında ilgili kullanıcıyı gerçekten
          (ders/not/devamsızlık/görev verisi dahil) siler.
        </p>
      </div>

      <section className="space-y-3">
        <h2 className="text-sm font-medium text-slate-700">
          Doğrulama Bekleniyor ({awaitingVerification.length})
        </h2>
        <p className="text-xs text-slate-400">
          Bu adreslere onay e-postası gönderildi; kişi linke tıklayana kadar
          burada bir işlem yapılamaz.
        </p>
        {awaitingVerification.length === 0 && (
          <p className="text-sm text-slate-400">Bekleyen talep yok.</p>
        )}
        {awaitingVerification.map((item) => (
          <div
            key={item.id}
            className="rounded-lg border border-slate-200 bg-slate-50 p-4"
          >
            <p className="text-sm font-medium text-slate-900">{item.email}</p>
            {item.reason && <p className="mt-1 text-sm text-slate-600">{item.reason}</p>}
            <p className="mt-1 text-xs text-slate-400">
              Talep: {item.createdAt.toLocaleString("tr-TR")}
            </p>
          </div>
        ))}
      </section>

      <section className="space-y-3">
        <h2 className="text-sm font-medium text-slate-700">
          Doğrulandı — Silinmeyi Bekliyor ({verifiedPending.length})
        </h2>
        {verifiedPending.length === 0 && (
          <p className="text-sm text-slate-400">Doğrulanmış bekleyen talep yok.</p>
        )}
        {verifiedPending.map((item) => (
          <div
            key={item.id}
            className="flex items-start justify-between gap-4 rounded-lg border border-amber-200 bg-amber-50 p-4"
          >
            <div>
              <p className="text-sm font-medium text-slate-900">{item.email}</p>
              {item.reason && <p className="mt-1 text-sm text-slate-600">{item.reason}</p>}
              <p className="mt-1 text-xs text-slate-400">
                Talep: {item.createdAt.toLocaleString("tr-TR")} · Onaylandı:{" "}
                {item.verifiedAt?.toLocaleString("tr-TR")}
              </p>
            </div>
            <form action={deleteVerifiedRequest.bind(null, item.id)}>
              <button
                type="submit"
                className="whitespace-nowrap rounded-md border border-red-300 bg-white px-3 py-1 text-xs text-red-700 hover:bg-red-50"
              >
                Kalıcı Olarak Sil
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
