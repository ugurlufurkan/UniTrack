/**
 * Elle admin hesabı oluşturma/şifre sıfırlama script'i.
 * Uygulamada kayıt formu YOK (emailAndPassword.disableSignUp: true) — bilerek.
 * Sadece 1-2 sabit admin olacağı için hesaplar buradan açılıyor.
 *
 * Kullanım:
 *   npm run create-admin -- "ad@ornek.com" "GucluBirSifre123" "Mehmet Doğan"
 */
import "dotenv/config";
import { randomUUID } from "crypto";
import { eq } from "drizzle-orm";
import { hashPassword } from "better-auth/crypto";

import { db } from "../lib/db";
import { adminAccount, adminUser } from "../lib/db/schema";

async function main() {
  const [email, password, name] = process.argv.slice(2);

  if (!email || !password) {
    console.error('Kullanım: npm run create-admin -- "e-posta" "şifre" "İsim (opsiyonel)"');
    process.exit(1);
  }

  if (password.length < 8) {
    console.error("Şifre en az 8 karakter olmalı.");
    process.exit(1);
  }

  const hashed = await hashPassword(password);

  const existing = await db.query.adminUser.findFirst({
    where: eq(adminUser.email, email),
  });

  if (existing) {
    // Hesap zaten varsa şifresini güncelle (sıfırlama akışı yerine bu kullanılabilir).
    await db
      .update(adminAccount)
      .set({ password: hashed, updatedAt: new Date() })
      .where(eq(adminAccount.userId, existing.id));

    console.log(`"${email}" hesabının şifresi güncellendi.`);
    return;
  }

  const userId = randomUUID();

  await db.insert(adminUser).values({
    id: userId,
    email,
    name: name || email.split("@")[0],
    emailVerified: true,
  });

  await db.insert(adminAccount).values({
    id: randomUUID(),
    userId,
    accountId: userId,
    providerId: "credential",
    password: hashed,
  });

  console.log(`Admin hesabı oluşturuldu: ${email}`);
}

main()
  .then(() => process.exit(0))
  .catch((err) => {
    console.error(err);
    process.exit(1);
  });
