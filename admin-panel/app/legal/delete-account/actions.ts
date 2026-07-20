"use server";

import { db } from "@/lib/db";
import { accountDeletionRequests } from "@/lib/db/schema";

const EMAIL_PATTERN = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;

export type DeleteAccountFormState = {
  status: "idle" | "success" | "error";
  message?: string;
};

export async function submitDeletionRequest(
  _prevState: DeleteAccountFormState,
  formData: FormData
): Promise<DeleteAccountFormState> {
  const email = String(formData.get("email") ?? "").trim().toLowerCase();
  const reason = String(formData.get("reason") ?? "").trim();

  if (!EMAIL_PATTERN.test(email)) {
    return { status: "error", message: "Geçerli bir e-posta adresi gir." };
  }

  await db.insert(accountDeletionRequests).values({
    email,
    reason: reason || null,
  });

  return { status: "success" };
}
