ALTER TABLE "attendance_records" ADD COLUMN "attended_hours" integer DEFAULT 0 NOT NULL;--> statement-breakpoint
ALTER TABLE "attendance_records" ADD COLUMN "absent_hours" integer DEFAULT 0 NOT NULL;--> statement-breakpoint
ALTER TABLE "attendance_records" ADD COLUMN "excused_hours" integer DEFAULT 0 NOT NULL;--> statement-breakpoint
ALTER TABLE "courses" ADD COLUMN "weekly_hours" integer DEFAULT 3 NOT NULL;--> statement-breakpoint
ALTER TABLE "courses" ADD COLUMN "attendance_limit_hours" integer DEFAULT 0 NOT NULL;--> statement-breakpoint
ALTER TABLE "users" ADD COLUMN "theme_preference" varchar(10);--> statement-breakpoint
ALTER TABLE "users" ADD COLUMN "target_gpa" real;--> statement-breakpoint
ALTER TABLE "users" ADD COLUMN "exam_period_start" timestamp;--> statement-breakpoint
ALTER TABLE "users" ADD COLUMN "exam_period_end" timestamp;--> statement-breakpoint
ALTER TABLE "attendance_records" DROP COLUMN "status";