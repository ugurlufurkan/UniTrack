"use server";

import { randomBytes } from "crypto";

import { db } from "@/lib/db";
import { accountDeletionRequests } from "@/lib/db/schema";
import { deletionConfirmationEmail, sendEmail } from "@/lib/email";

const EMAIL_PATTERN = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
const TOKEN_TTL_MS = 1000 * 60 * 60 * 24; // 24 saat

export type DeleteAccountFormState = {
  status: "idle" | "success" | "error";
  message?: string;
};

// Onay linkini oluştururken kullanılan taban adres — admin panelin kendi
// URL'i (.env.example'da zaten "admin panelin kendi adresi" olarak tanımlı).
function getBaseUrl() {
  return process.env.NEXT_PUBLIC_BETTER_AUTH_URL || "http://localhost:3000";
}

export async function submitDeletionRequest(
  _prevState: DeleteAccountFormState,
  formData: FormData
): Promise<DeleteAccountFormState> {
  const email = String(formData.get("email") ?? "").trim().toLowerCase();
  const reason = String(formData.get("reason") ?? "").trim();

  if (!EMAIL_PATTERN.test(email)) {
    return { status: "error", message: "Geçerli bir e-posta adresi gir." };
  }

  // Not: Bilerek "bu e-posta zaten kayıtlı mı" diye bir kontrol YAPMIYORUZ —
  // öyle bir kontrol saldırgana hangi e-postaların sistemde olduğunu
  // sızdırırdı (email enumeration). Talep her koşulda oluşturulur; asıl
  // güvenlik, onay e-postasındaki tek kullanımlık token'dan geliyor —
  // e-postaya gerçekten erişimi olmayan biri linke hiç tıklayamaz.
  const token = randomBytes(32).toString("hex");
  const expiresAt = new Date(Date.now() + TOKEN_TTL_MS);

  await db.insert(accountDeletionRequests).values({
    email,
    reason: reason || null,
    verificationToken: token,
    verificationTokenExpiresAt: expiresAt,
  });

  const confirmUrl = `${getBaseUrl()}/legal/delete-account/confirm?token=${token}`;
  await sendEmail({
    to: email,
    subject: "UniTrack hesap silme talebini onayla",
    html: deletionConfirmationEmail(confirmUrl),
  });

  // Talep oluşsun ya da e-posta hiç kayıtlı olmasın, kullanıcıya AYNI mesajı
  // gösteriyoruz — farklı bir mesaj yine email enumeration'a kapı açardı.
  return {
    status: "success",
    message:
      "Eğer bu adres sistemimizde kayıtlıysa, onay bağlantısı gönderildi. Gelen kutunu (ve spam klasörünü) kontrol et.",
  };
}
