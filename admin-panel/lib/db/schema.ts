import { boolean, integer, pgTable, text, timestamp, uuid, varchar } from "drizzle-orm/pg-core";

/* =========================================================
   BETTER-AUTH TABLOLARI (sadece admin girişleri için)
   ---------------------------------------------------------
   Bilerek "admin_" önekiyle adlandırıldı: bu veritabanı UniTrack mobil
   backend'i ile PAYLAŞILIYOR ve orada zaten öğrenciler için "users" adlı
   bambaşka bir tablo var. Better-auth'un varsayılan "user"/"session" gibi
   isimleriyle çakışmasın diye tabloları burada admin_* olarak tanımlayıp
   auth.ts içinde better-auth'a "user tablon aslında bu" diye eşliyoruz.
   ========================================================= */

export const adminUser = pgTable("admin_users", {
  id: text("id").primaryKey(),
  name: text("name").notNull(),
  email: text("email").notNull().unique(),
  emailVerified: boolean("email_verified").notNull().default(false),
  image: text("image"),
  createdAt: timestamp("created_at").notNull().defaultNow(),
  updatedAt: timestamp("updated_at").notNull().defaultNow(),
});

export const adminSession = pgTable("admin_sessions", {
  id: text("id").primaryKey(),
  expiresAt: timestamp("expires_at").notNull(),
  token: text("token").notNull().unique(),
  createdAt: timestamp("created_at").notNull().defaultNow(),
  updatedAt: timestamp("updated_at").notNull().defaultNow(),
  ipAddress: text("ip_address"),
  userAgent: text("user_agent"),
  userId: text("user_id")
    .notNull()
    .references(() => adminUser.id, { onDelete: "cascade" }),
});

export const adminAccount = pgTable("admin_accounts", {
  id: text("id").primaryKey(),
  accountId: text("account_id").notNull(),
  providerId: text("provider_id").notNull(),
  userId: text("user_id")
    .notNull()
    .references(() => adminUser.id, { onDelete: "cascade" }),
  accessToken: text("access_token"),
  refreshToken: text("refresh_token"),
  idToken: text("id_token"),
  accessTokenExpiresAt: timestamp("access_token_expires_at"),
  refreshTokenExpiresAt: timestamp("refresh_token_expires_at"),
  scope: text("scope"),
  password: text("password"),
  createdAt: timestamp("created_at").notNull().defaultNow(),
  updatedAt: timestamp("updated_at").notNull().defaultNow(),
});

export const adminVerification = pgTable("admin_verifications", {
  id: text("id").primaryKey(),
  identifier: text("identifier").notNull(),
  value: text("value").notNull(),
  expiresAt: timestamp("expires_at").notNull(),
  createdAt: timestamp("created_at").defaultNow(),
  updatedAt: timestamp("updated_at").defaultNow(),
});

/* =========================================================
   İÇERİK TABLOLARI
   ---------------------------------------------------------
   Bunlar mobil backend'in (UniTrack/backend/src/db/schema.ts) tanımladığı
   TABLOLARLA AYNI (isim/kolon birebir eşleşiyor) — çünkü aynı Postgres
   veritabanını paylaşıyoruz. Mobil backend bu tabloları SADECE okuyor
   (GET /api/v1/content/*), yazma/silme/düzenleme burada, admin panelinde.

   ÖNEMLİ: Bu iki dosya (buradaki ve backend'deki) elle senkron tutulmalı.
   Kolon eklemek/çıkarmak isterseniz HER İKİ schema.ts'i de güncelleyip
   her iki projede de migration üretmeniz gerekir.
   ========================================================= */

export const announcements = pgTable("announcements", {
  id: uuid("id").defaultRandom().primaryKey(),
  title: varchar("title", { length: 200 }).notNull(),
  body: text("body").notNull(),
  isActive: boolean("is_active").notNull().default(true),
  publishedAt: timestamp("published_at"),
  createdAt: timestamp("created_at").notNull().defaultNow(),
  updatedAt: timestamp("updated_at").notNull().defaultNow(),
});

export const faqs = pgTable("faqs", {
  id: uuid("id").defaultRandom().primaryKey(),
  question: varchar("question", { length: 300 }).notNull(),
  answer: text("answer").notNull(),
  sortOrder: integer("sort_order").notNull().default(0),
  isActive: boolean("is_active").notNull().default(true),
  createdAt: timestamp("created_at").notNull().defaultNow(),
  updatedAt: timestamp("updated_at").notNull().defaultNow(),
});

export const tips = pgTable("tips", {
  id: uuid("id").defaultRandom().primaryKey(),
  message: text("message").notNull(),
  sortOrder: integer("sort_order").notNull().default(0),
  isActive: boolean("is_active").notNull().default(true),
  createdAt: timestamp("created_at").notNull().defaultNow(),
  updatedAt: timestamp("updated_at").notNull().defaultNow(),
});

export const staticPages = pgTable("static_pages", {
  id: uuid("id").defaultRandom().primaryKey(),
  slug: varchar("slug", { length: 100 }).notNull().unique(),
  title: varchar("title", { length: 200 }).notNull(),
  content: text("content").notNull(),
  updatedAt: timestamp("updated_at").notNull().defaultNow(),
});

/* =========================================================
   HESAP SİLME TALEPLERİ (Google Play "Account deletion" şartı)
   ---------------------------------------------------------
   Play, hesap oluşturmaya izin veren uygulamalardan hem UYGULAMA İÇİNDE
   hem de UYGULAMAYI SİLMİŞ/YÜKLEMEMİŞ biri için AYRICA bir WEB sayfasından
   silme talebi alınabilmesini istiyor (bkz. app/legal/delete-account).

   Akış:
   1. Web formuna e-posta girilir -> bu tabloya "pending", verifiedAt=null
      olarak kaydedilir ve o adrese bir onay linki gönderilir.
   2. Kişi linke tıklayınca verifiedAt dolar. Buraya kadar hiçbir veri
      silinmiyor — bu adım sadece "bu e-postanın gerçekten sahibiyim"
      doğrulaması (yoksa herkes başkasının e-postasıyla sahte talep açıp
      onun hesabını sildirebilirdi).
   3. Admin panelinde SADECE doğrulanmış (verifiedAt dolu) talepler için
      "Kalıcı Olarak Sil" butonu aktif olur; tıklanınca mobil backend'in
      `users` tablosundan o e-postayla eşleşen kullanıcı GERÇEKTEN silinir
      (bkz. deletion-requests/actions.ts) — cascade FK'ler sayesinde tüm
      ders/not/devamsızlık/görev verisi de otomatik gider.
   ========================================================= */

export const accountDeletionRequests = pgTable("account_deletion_requests", {
  id: uuid("id").defaultRandom().primaryKey(),
  email: varchar("email", { length: 255 }).notNull(),
  reason: text("reason"),
  status: varchar("status", { length: 20 }).notNull().default("pending"), // pending | completed
  createdAt: timestamp("created_at").notNull().defaultNow(),
  processedAt: timestamp("processed_at"),

  // E-posta doğrulama: bu olmadan herkes başkasının e-postasıyla sahte
  // silme talebi açabilirdi. Talep oluşturulunca bu adrese bir onay linki
  // gönderiliyor; kullanıcı linke tıklayana kadar verifiedAt null kalıyor
  // ve admin panelindeki "Kalıcı olarak sil" butonu bu talep için AÇILMIYOR.
  verificationToken: varchar("verification_token", { length: 128 }),
  verificationTokenExpiresAt: timestamp("verification_token_expires_at"),
  verifiedAt: timestamp("verified_at"),
});
