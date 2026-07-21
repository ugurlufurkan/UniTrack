"use server";

import { eq, sql } from "drizzle-orm";
import { revalidatePath } from "next/cache";

import { db } from "@/lib/db";
import { accountDeletionRequests } from "@/lib/db/schema";
import { requireAdmin } from "@/lib/require-admin";

/**
 * Doğrulanmış (verifiedAt dolu) bir talebi GERÇEKTEN uygular: mobil
 * backend'in `users` tablosundan o e-postayla eşleşen kullanıcıyı siler.
 * Admin panelinin kendi drizzle şeması `users` tablosunu tanımlamıyor
 * (mobil backend'in şeması zaten tanımlıyor, iki tarafta aynı tabloyu iki
 * ayrı TS tanımıyla senkron tutmaya gerek yok) — bu yüzden ham SQL
 * kullanıyoruz. Yine de güvenli: `users` tablosundaki her FK
 * ("onDelete: cascade") Postgres seviyesinde tanımlı, TS tarafında tabloyu
 * tanıyıp tanımadığımızdan bağımsız çalışır.
 *
 * Bu buton admin panelinde SADECE verifiedAt dolu talepler için render
 * ediliyor (bkz. page.tsx), ama yine de burada tekrar kontrol ediyoruz —
 * ön yüzdeki kontrole güvenmek yerine.
 */
export async function deleteVerifiedRequest(id: string) {
  await requireAdmin();

  const [request] = await db
    .select()
    .from(accountDeletionRequests)
    .where(eq(accountDeletionRequests.id, id))
    .limit(1);

  if (!request || !request.verifiedAt) {
    // Buton zaten sadece doğrulanmış talepler için gösteriliyor; buraya
    // düşülmesi normalde mümkün değil. Yine de sessizce çıkıyoruz —
    // plain <form action> ile çağrıldığı için burada fırlatılan bir hata
    // kullanıcıya çirkin bir Next.js hata sayfası gösterirdi.
    return;
  }

  await db.execute(sql`DELETE FROM users WHERE lower(email) = lower(${request.email})`);

  await db
    .update(accountDeletionRequests)
    .set({ status: "completed", processedAt: new Date() })
    .where(eq(accountDeletionRequests.id, id));

  revalidatePath("/deletion-requests");
}
