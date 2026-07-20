"use server";

import { eq } from "drizzle-orm";
import { revalidatePath } from "next/cache";

import { db } from "@/lib/db";
import { faqs } from "@/lib/db/schema";
import { requireAdmin } from "@/lib/require-admin";

export async function createFaq(formData: FormData) {
  await requireAdmin();

  const question = String(formData.get("question") ?? "").trim();
  const answer = String(formData.get("answer") ?? "").trim();

  if (!question || !answer) return;

  await db.insert(faqs).values({ question, answer });
  revalidatePath("/faqs");
}

export async function updateFaq(id: string, formData: FormData) {
  await requireAdmin();

  const question = String(formData.get("question") ?? "").trim();
  const answer = String(formData.get("answer") ?? "").trim();
  const sortOrder = Number(formData.get("sortOrder") ?? 0);
  const isActive = formData.get("isActive") === "on";

  if (!question || !answer) return;

  await db
    .update(faqs)
    .set({ question, answer, sortOrder, isActive, updatedAt: new Date() })
    .where(eq(faqs.id, id));

  revalidatePath("/faqs");
}

export async function deleteFaq(id: string) {
  await requireAdmin();
  await db.delete(faqs).where(eq(faqs.id, id));
  revalidatePath("/faqs");
}
