"use server";

import { eq } from "drizzle-orm";
import { revalidatePath } from "next/cache";

import { db } from "@/lib/db";
import { staticPages } from "@/lib/db/schema";
import { requireAdmin } from "@/lib/require-admin";

const SLUG_PATTERN = /^[a-z0-9-]+$/;

export async function createPage(formData: FormData) {
  await requireAdmin();

  const slug = String(formData.get("slug") ?? "").trim().toLowerCase();
  const title = String(formData.get("title") ?? "").trim();
  const content = String(formData.get("content") ?? "").trim();

  if (!slug || !title || !content || !SLUG_PATTERN.test(slug)) return;

  await db.insert(staticPages).values({ slug, title, content });
  revalidatePath("/pages");
}

export async function updatePage(id: string, formData: FormData) {
  await requireAdmin();

  const title = String(formData.get("title") ?? "").trim();
  const content = String(formData.get("content") ?? "").trim();

  if (!title || !content) return;

  await db
    .update(staticPages)
    .set({ title, content, updatedAt: new Date() })
    .where(eq(staticPages.id, id));

  revalidatePath("/pages");
}

export async function deletePage(id: string) {
  await requireAdmin();
  await db.delete(staticPages).where(eq(staticPages.id, id));
  revalidatePath("/pages");
}
