"use server";

import { eq } from "drizzle-orm";
import { revalidatePath } from "next/cache";

import { db } from "@/lib/db";
import { announcements } from "@/lib/db/schema";
import { requireAdmin } from "@/lib/require-admin";

export async function createAnnouncement(formData: FormData) {
  await requireAdmin();

  const title = String(formData.get("title") ?? "").trim();
  const body = String(formData.get("body") ?? "").trim();

  if (!title || !body) return;

  await db.insert(announcements).values({ title, body });
  revalidatePath("/announcements");
}

export async function updateAnnouncement(id: string, formData: FormData) {
  await requireAdmin();

  const title = String(formData.get("title") ?? "").trim();
  const body = String(formData.get("body") ?? "").trim();
  const isActive = formData.get("isActive") === "on";

  if (!title || !body) return;

  await db
    .update(announcements)
    .set({ title, body, isActive, updatedAt: new Date() })
    .where(eq(announcements.id, id));

  revalidatePath("/announcements");
}

export async function deleteAnnouncement(id: string) {
  await requireAdmin();
  await db.delete(announcements).where(eq(announcements.id, id));
  revalidatePath("/announcements");
}
