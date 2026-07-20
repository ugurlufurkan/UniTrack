"use server";

import { eq } from "drizzle-orm";
import { revalidatePath } from "next/cache";

import { db } from "@/lib/db";
import { tips } from "@/lib/db/schema";
import { requireAdmin } from "@/lib/require-admin";

export async function createTip(formData: FormData) {
  await requireAdmin();

  const message = String(formData.get("message") ?? "").trim();
  if (!message) return;

  await db.insert(tips).values({ message });
  revalidatePath("/tips");
}

export async function updateTip(id: string, formData: FormData) {
  await requireAdmin();

  const message = String(formData.get("message") ?? "").trim();
  const sortOrder = Number(formData.get("sortOrder") ?? 0);
  const isActive = formData.get("isActive") === "on";

  if (!message) return;

  await db
    .update(tips)
    .set({ message, sortOrder, isActive, updatedAt: new Date() })
    .where(eq(tips.id, id));

  revalidatePath("/tips");
}

export async function deleteTip(id: string) {
  await requireAdmin();
  await db.delete(tips).where(eq(tips.id, id));
  revalidatePath("/tips");
}
