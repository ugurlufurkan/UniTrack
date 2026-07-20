import {
  pgTable,
  uuid,
  varchar,
  integer,
  timestamp,
  text,
  real,
  boolean,
  jsonb,
} from "drizzle-orm/pg-core";
import { sql } from "drizzle-orm";

/* =========================
   GRADE SCALE (harf notu aralığı)
   Örn: [{ letter: "AA", min: 90, point: 4.0 }, ...]
   null ise sistem varsayılanı kullanılır.
========================= */

export interface GradeBand {
  letter: string;
  min: number;
  point: number;
}

/* =========================
   USERS
========================= */

export const users = pgTable("users", {
  id: uuid("id").defaultRandom().primaryKey(),

  googleId: varchar("google_id", {
    length: 255,
  }),

  email: varchar("email", {
    length: 255,
  })
    .notNull()
    .unique(),

  password: varchar("password", {
    length: 255,
  }),

  name: varchar("name", {
    length: 100,
  }).notNull(),

  picture: text("picture"),

  // Email verification
  isEmailVerified: boolean("is_email_verified").default(false),
  emailVerificationToken: varchar("email_verification_token", { length: 255 }),
  emailVerificationExpiresAt: timestamp("email_verification_expires_at"),

  // Password reset
  passwordResetToken: varchar("password_reset_token", { length: 255 }),
  passwordResetExpiresAt: timestamp("password_reset_expires_at"),

  // Kullanıcının genel/varsayılan harf notu skalası. null ise sistem
  // varsayılanı (AA/BA/BB.../FF) kullanılır. Dersler bunu tek tek ezebilir.
  defaultGradeScale: jsonb("default_grade_scale").$type<GradeBand[]>(),

  /* =========================
     CİHAZLAR ARASI SENKRON EDİLEN KİŞİSEL AYARLAR
     Daha önce sadece cihazda (DataStore) tutulan tema/hedef GANO/sınav
     haftası ayarları — telefon değişince kaybolmasınlar diye artık burada.
     Hepsi nullable: null = kullanıcı hiç ayarlamamış, istemci kendi
     varsayılanını (SYSTEM tema, boş hedef, boş tarih) kullanır.
  ========================= */

  themePreference: varchar("theme_preference", { length: 10 }), // SYSTEM | LIGHT | DARK

  targetGpa: real("target_gpa"),

  examPeriodStart: timestamp("exam_period_start"),
  examPeriodEnd: timestamp("exam_period_end"),

  createdAt: timestamp("created_at")
    .defaultNow()
    .notNull(),
  
  updatedAt: timestamp("updated_at")
    .defaultNow()
    .notNull(),
});

/* =========================
   SEMESTERS
========================= */

export const semesters = pgTable("semesters", {
  id: uuid("id").defaultRandom().primaryKey(),

  userId: uuid("user_id")
    .references(() => users.id, {
      onDelete: "cascade",
    })
    .notNull(),

  year: integer("year").notNull(),

  term: varchar("term", {
    length: 20,
  }).notNull(),

  createdAt: timestamp("created_at")
    .defaultNow()
    .notNull(),
});

/* =========================
   COURSES
========================= */

export const courses = pgTable("courses", {
  id: uuid("id").defaultRandom().primaryKey(),

  semesterId: uuid("semester_id")
    .references(() => semesters.id, {
      onDelete: "cascade",
    })
    .notNull(),

  name: varchar("name", {
    length: 150,
  }).notNull(),

  credit: integer("credit").notNull(),

  // Bu derse özel harf notu skalası. null ise kullanıcının genel
  // varsayılanı (o da null ise sistem varsayılanı) kullanılır.
  gradeScale: jsonb("grade_scale").$type<GradeBand[]>(),

  average: real("average"),

  letterGrade: varchar("letter_grade", {
    length: 2,
  }),

  gradePoint: real("grade_point"),

  passed: boolean("passed").default(false),

  // Devamsizlik takibi icin: bu dersin toplam hafta sayisi (varsayilan 14,
  // kullanici isterse 12 vb. bir degere guncelleyebilir).
  totalWeeks: integer("total_weeks").notNull().default(14),

  // Devamsizlik saat bazli takip icin: bu dersin haftalik ders saati
  // (orn. Mat1 haftada 4 saat) ve toplam devamsizlik limiti (saat).
  // Ikisi de kullanici tarafindan ders ayarlarindan degistirilebilir.
  weeklyHours: integer("weekly_hours").notNull().default(3),
  attendanceLimitHours: integer("attendance_limit_hours").notNull().default(0),

  createdAt: timestamp("created_at")
    .defaultNow()
    .notNull(),
});

/* =========================
   COURSE COMPONENTS
   (Vize/Final/Büt yerine serbest bileşenler: Vize, Final, Proje, Quiz vb.
   Her bileşenin bir ağırlığı (%) ve bir puanı (0-100) vardır.)
========================= */

export const courseComponents = pgTable("course_components", {
  id: uuid("id").defaultRandom().primaryKey(),

  courseId: uuid("course_id")
    .references(() => courses.id, {
      onDelete: "cascade",
    })
    .notNull(),

  name: varchar("name", {
    length: 100,
  }).notNull(),

  // Yüzde ağırlık (0-100). Bir dersin tüm bileşenlerinin toplamı 100 olmalı.
  weight: real("weight").notNull(),

  // Puan girilmemişse ders "devam ediyor" sayılır.
  score: real("score"),

  sortOrder: integer("sort_order").notNull().default(0),

  createdAt: timestamp("created_at")
    .defaultNow()
    .notNull(),
});

/* =========================
   REFRESH TOKENS (with device tracking)
========================= */

export const refreshTokens = pgTable("refresh_tokens", {
  id: uuid("id").defaultRandom().primaryKey(),

  userId: uuid("user_id")
    .references(() => users.id, {
      onDelete: "cascade",
    })
    .notNull(),

  token: text("token")
    .notNull()
    .unique(),

  // Device information
  deviceName: varchar("device_name", { length: 255 }),
  deviceType: varchar("device_type", { length: 50 }), // mobile, desktop, tablet
  userAgent: text("user_agent"),
  ipAddress: varchar("ip_address", { length: 45 }),

  expiresAt: timestamp("expires_at").notNull(),

  revokedAt: timestamp("revoked_at"),

  replacedBy: text("replaced_by"),

  // For reuse detection
  isReused: boolean("is_reused").default(false),

  createdAt: timestamp("created_at")
    .defaultNow()
    .notNull(),
  
  lastUsedAt: timestamp("last_used_at").defaultNow(),
});

/* =========================
   AUDIT LOGS
========================= */

/* =========================
   CALENDAR EVENTS
========================= */

export const events = pgTable("events", {
  id: uuid("id").defaultRandom().primaryKey(),

  userId: uuid("user_id")
    .references(() => users.id, { onDelete: "cascade" })
    .notNull(),

  courseId: uuid("course_id").references(() => courses.id, {
    onDelete: "set null",
  }),

  title: varchar("title", { length: 200 }).notNull(),

  description: text("description"),

  // lesson | exam | quiz | assignment | project | presentation | other
  type: varchar("type", { length: 30 }).notNull(),

  startAt: timestamp("start_at").notNull(),

  endAt: timestamp("end_at"),

  location: varchar("location", { length: 255 }),

  // low | medium | high
  priority: varchar("priority", { length: 20 }).notNull().default("medium"),

  // pending | in_progress | completed | cancelled
  status: varchar("status", { length: 20 }).notNull().default("pending"),

  color: varchar("color", { length: 20 }).notNull().default("#6366F1"),

  // none | daily | weekly | monthly
  recurrence: varchar("recurrence", { length: 20 }).notNull().default("none"),

  notificationsEnabled: boolean("notifications_enabled").default(true).notNull(),

  createdAt: timestamp("created_at").defaultNow().notNull(),

  updatedAt: timestamp("updated_at").defaultNow().notNull(),
});

/* =========================
   EVENT NOTIFICATIONS
========================= */

export const eventNotifications = pgTable("event_notifications", {
  id: uuid("id").defaultRandom().primaryKey(),

  eventId: uuid("event_id")
    .references(() => events.id, { onDelete: "cascade" })
    .notNull(),

  daysBefore: integer("days_before").notNull().default(0),

  hoursBefore: integer("hours_before").notNull().default(0),

  minutesBefore: integer("minutes_before").notNull().default(0),

  createdAt: timestamp("created_at").defaultNow().notNull(),
});

/* =========================
   COURSE SCHEDULE (haftalık ders programı)
========================= */

export const courseSchedule = pgTable("course_schedule", {
  id: uuid("id").defaultRandom().primaryKey(),

  userId: uuid("user_id")
    .references(() => users.id, { onDelete: "cascade" })
    .notNull(),

  courseId: uuid("course_id")
    .references(() => courses.id, { onDelete: "cascade" })
    .notNull(),

  // 0 = Pazar, 1 = Pazartesi, ... 6 = Cumartesi
  dayOfWeek: integer("day_of_week").notNull(),

  startTime: varchar("start_time", { length: 5 }).notNull(),

  endTime: varchar("end_time", { length: 5 }).notNull(),

  location: varchar("location", { length: 255 }),

  createdAt: timestamp("created_at").defaultNow().notNull(),
});

/* =========================
   AUDIT LOGS
========================= */

export const auditLogs = pgTable("audit_logs", {
  id: uuid("id").defaultRandom().primaryKey(),

  userId: uuid("user_id").references(() => users.id, {
    onDelete: "set null",
  }),

  action: varchar("action", { length: 100 }).notNull(), // LOGIN, LOGOUT, PASSWORD_CHANGE, etc.
  entity: varchar("entity", { length: 100 }), // USER, COURSE, SEMESTER, etc.
  entityId: varchar("entity_id", { length: 255 }),

  // Request details
  method: varchar("method", { length: 10 }),
  path: varchar("path", { length: 500 }),
  statusCode: integer("status_code"),

  // Device information
  userAgent: text("user_agent"),
  ipAddress: varchar("ip_address", { length: 45 }),

  // Additional metadata
  metadata: jsonb("metadata"),

  createdAt: timestamp("created_at")
    .defaultNow()
    .notNull(),
});
/* =========================
   ATTENDANCE RECORDS (devamsizlik takibi)
   Her ders + hafta icin bir kayit (upsert). status: present | absent | excused
========================= */

export const attendanceRecords = pgTable("attendance_records", {
  id: uuid("id").defaultRandom().primaryKey(),

  userId: uuid("user_id")
    .references(() => users.id, { onDelete: "cascade" })
    .notNull(),

  courseId: uuid("course_id")
    .references(() => courses.id, { onDelete: "cascade" })
    .notNull(),

  // 1 .. course.totalWeeks
  weekNumber: integer("week_number").notNull(),

  date: timestamp("date").notNull(),

  // O hafta ders kac saatti / ogrenci kacina katildi / kacina katilmadi /
  // kacina izinliydi. Ucu toplami course.weeklyHours'u gecemez.
  attendedHours: integer("attended_hours").notNull().default(0),
  absentHours: integer("absent_hours").notNull().default(0),
  excusedHours: integer("excused_hours").notNull().default(0),

  note: text("note"),

  createdAt: timestamp("created_at").defaultNow().notNull(),

  updatedAt: timestamp("updated_at").defaultNow().notNull(),
});

/* =========================
   EVENT CHECKLIST ITEMS (gorev yonetimi: odev/proje/sunum alt adimlari)
========================= */

export const eventChecklistItems = pgTable("event_checklist_items", {
  id: uuid("id").defaultRandom().primaryKey(),

  eventId: uuid("event_id")
    .references(() => events.id, { onDelete: "cascade" })
    .notNull(),

  title: varchar("title", { length: 200 }).notNull(),

  isDone: boolean("is_done").default(false).notNull(),

  sortOrder: integer("sort_order").notNull().default(0),

  createdAt: timestamp("created_at").defaultNow().notNull(),

  updatedAt: timestamp("updated_at").defaultNow().notNull(),
});

/* =========================
   İÇERİK YÖNETİMİ (admin panelinden düzenlenir, APK güncellemesi
   gerektirmez). Bu tablolar ayrıca admin-panel/ (Next.js + better-auth)
   uygulaması tarafından da Drizzle ile doğrudan okunup yazılır — iki
   uygulama da AYNI Postgres veritabanını paylaşır.
========================= */

// Tüm kullanıcılara gösterilen duyurular (ör. "Sunucu bakımı", "Yeni özellik").
export const announcements = pgTable("announcements", {
  id: uuid("id").defaultRandom().primaryKey(),

  title: varchar("title", { length: 200 }).notNull(),
  body: text("body").notNull(),

  isActive: boolean("is_active").default(true).notNull(),

  // Admin ileri tarihli bir duyuru hazırlayabilsin diye ayrı bir alan;
  // null ise createdAt anında yayınlanmış sayılır.
  publishedAt: timestamp("published_at"),

  createdAt: timestamp("created_at").defaultNow().notNull(),
  updatedAt: timestamp("updated_at").defaultNow().notNull(),
});

// Sıkça Sorulan Sorular.
export const faqs = pgTable("faqs", {
  id: uuid("id").defaultRandom().primaryKey(),

  question: varchar("question", { length: 300 }).notNull(),
  answer: text("answer").notNull(),

  sortOrder: integer("sort_order").notNull().default(0),
  isActive: boolean("is_active").default(true).notNull(),

  createdAt: timestamp("created_at").defaultNow().notNull(),
  updatedAt: timestamp("updated_at").defaultNow().notNull(),
});

// Dashboard'da dönen kısa ipucu/motivasyon mesajları.
export const tips = pgTable("tips", {
  id: uuid("id").defaultRandom().primaryKey(),

  message: text("message").notNull(),

  sortOrder: integer("sort_order").notNull().default(0),
  isActive: boolean("is_active").default(true).notNull(),

  createdAt: timestamp("created_at").defaultNow().notNull(),
  updatedAt: timestamp("updated_at").defaultNow().notNull(),
});

// Uygulama içi statik sayfalar (Hakkımızda, KVKK, Kullanım Şartları vb.).
// slug sabit kalır (ör. "about", "privacy", "terms"); admin sadece içeriği
// düzenler, uygulama tarafı slug'a göre çeker.
export const staticPages = pgTable("static_pages", {
  id: uuid("id").defaultRandom().primaryKey(),

  slug: varchar("slug", { length: 100 }).notNull().unique(),
  title: varchar("title", { length: 200 }).notNull(),
  content: text("content").notNull(), // Markdown

  updatedAt: timestamp("updated_at").defaultNow().notNull(),
});
