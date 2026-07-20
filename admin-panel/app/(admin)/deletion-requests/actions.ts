"use server";

import { eq } from "drizzle-orm";
import { revalidatePath } from "next/cache";

import { db } from "@/lib/db";
import { accountDeletionRequests } from "@/lib/db/schema";
import { requireAdmin } from "@/lib/require-admin";

export async function markCompleted(id: string) {
  await requireAdmin();
  await db
    .update(accountDeletionRequests)
    .set({ status: "completed", processedAt: new Date() })
    .where(eq(accountDeletionRequests.id, id));
  revalidatePath("/deletion-requests");
}
