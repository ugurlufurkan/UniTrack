import "dotenv/config";
import { defineConfig } from "drizzle-kit";

export default defineConfig({
  out: "./drizzle",
  schema: "./lib/db/schema.ts",
  dialect: "postgresql",
  dbCredentials: {
    url: process.env.DATABASE_URL!,
  },
  // KRİTİK: Bu veritabanı UniTrack mobil backend'iyle PAYLAŞILIYOR.
  // admin-panel'in schema.ts'i backend'e ait tabloları (users, courses,
  // attendance_records, refresh_tokens, audit_logs, semesters,
  // course_components...) hiç tanımlamıyor — bu yüzden `tablesFilter`
  // olmadan `drizzle-kit push`, DB'de görüp kendi şemasında bulamadığı bu
  // tabloları "silinecek" sanıyor. Bu liste, push/generate'in SADECE
  // admin-panel'in kendi sahip olduğu tabloları görmesini sağlıyor.
  // Yeni bir admin-panel tablosu eklersen buraya da adını ekle.
  tablesFilter: [
    "admin_users",
    "admin_sessions",
    "admin_accounts",
    "admin_verifications",
    "announcements",
    "faqs",
    "tips",
    "static_pages",
    "account_deletion_requests",
  ],
});
