import { and, desc, eq, isNull, lte, or } from "drizzle-orm";

import { db } from "../../db";
import { announcements, faqs, staticPages, tips } from "../../db/schema";
import { AppError } from "../../shared/errors/AppError";

// Yalnızca aktif ve (varsa) yayın tarihi gelmiş duyurular gösterilir.
// Admin panelinden ileri tarihli bir duyuru hazırlanabilir (publishedAt).
export const findActiveAnnouncements = async () => {
  const now = new Date();

  return db
    .select()
    .from(announcements)
    .where(
      and(
        eq(announcements.isActive, true),
        or(isNull(announcements.publishedAt), lte(announcements.publishedAt, now))
      )
    )
    .orderBy(desc(announcements.publishedAt), desc(announcements.createdAt));
};

export const findActiveFaqs = async () => {
  return db
    .select()
    .from(faqs)
    .where(eq(faqs.isActive, true))
    .orderBy(faqs.sortOrder);
};

export const findActiveTips = async () => {
  return db
    .select()
    .from(tips)
    .where(eq(tips.isActive, true))
    .orderBy(tips.sortOrder);
};

export const findPageBySlug = async (slug: string) => {
  const [page] = await db
    .select()
    .from(staticPages)
    .where(eq(staticPages.slug, slug))
    .limit(1);

  if (!page) {
    throw AppError.notFound("Sayfa bulunamadı.");
  }

  return page;
};
