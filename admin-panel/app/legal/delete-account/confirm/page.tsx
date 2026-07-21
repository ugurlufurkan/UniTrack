import Link from "next/link";
import { and, eq, gt, isNull } from "drizzle-orm";

import { db } from "@/lib/db";
import { accountDeletionRequests } from "@/lib/db/schema";

export const dynamic = "force-dynamic";

export const metadata = {
  title: "Hesap Silme Talebi Onayı — UniTrack",
};

async function confirmToken(token: string): Promise<"ok" | "already" | "invalid"> {
  // Süresi geçmemiş VE daha önce onaylanmamış, token'ı eşleşen bir talep var mı?
  const [pending] = await db
    .select()
    .from(accountDeletionRequests)
    .where(
      and(
        eq(accountDeletionRequests.verificationToken, token),
        isNull(accountDeletionRequests.verifiedAt),
        gt(accountDeletionRequests.verificationTokenExpiresAt, new Date())
      )
    )
    .limit(1);

  if (pending) {
    await db
      .update(accountDeletionRequests)
      .set({ verifiedAt: new Date() })
      .where(eq(accountDeletionRequests.id, pending.id));
    return "ok";
  }

  // Token geçerli ama zaten daha önce onaylanmışsa (link'e 2 kere tıklanmış
  // olabilir) kullanıcıya hata değil, "zaten onaylandı" göstermek daha doğru.
  const [already] = await db
    .select()
    .from(accountDeletionRequests)
    .where(eq(accountDeletionRequests.verificationToken, token))
    .limit(1);

  if (already?.verifiedAt) {
    return "already";
  }

  return "invalid";
}

export default async function ConfirmDeletePage({
  searchParams,
}: {
  searchParams: Promise<{ token?: string }>;
}) {
  const { token } = await searchParams;
  const result = token ? await confirmToken(token) : "invalid";

  return (
    <div className="min-h-screen bg-white">
      <div className="mx-auto max-w-xl px-6 py-12">
        <Link href="/legal" className="text-sm text-slate-400 hover:text-slate-600">
          ← Tüm sayfalar
        </Link>

        {result === "ok" && (
          <div className="mt-6 rounded-lg border border-green-200 bg-green-50 p-4 text-sm text-green-800">
            <p className="font-medium">Talebin onaylandı.</p>
            <p className="mt-1">
              Hesabın ve ilişkili tüm verilerin kısa süre içinde ekibimiz tarafından
              kalıcı olarak silinecek.
            </p>
          </div>
        )}

        {result === "already" && (
          <div className="mt-6 rounded-lg border border-slate-200 bg-slate-50 p-4 text-sm text-slate-700">
            Bu talep zaten daha önce onaylandı — tekrar bir şey yapmana gerek yok.
          </div>
        )}

        {result === "invalid" && (
          <div className="mt-6 rounded-lg border border-red-200 bg-red-50 p-4 text-sm text-red-800">
            <p className="font-medium">Bu bağlantı geçersiz ya da süresi dolmuş.</p>
            <p className="mt-1">
              Onay bağlantıları 24 saat sonra geçersiz olur.{" "}
              <Link href="/legal/delete-account" className="underline">
                Yeni bir talep gönder
              </Link>
              .
            </p>
          </div>
        )}
      </div>
    </div>
  );
}
