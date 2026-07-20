ALTER TABLE "courses" ADD COLUMN "total_weeks" integer DEFAULT 14 NOT NULL;
--> statement-breakpoint
ALTER TABLE "courses" ADD COLUMN "attendance_limit_percent" real DEFAULT 25 NOT NULL;
--> statement-breakpoint
CREATE TABLE "attendance_records" (
	"id" uuid PRIMARY KEY DEFAULT gen_random_uuid() NOT NULL,
	"user_id" uuid NOT NULL,
	"course_id" uuid NOT NULL,
	"week_number" integer NOT NULL,
	"date" timestamp,
	"status" varchar(20) DEFAULT 'attended' NOT NULL,
	"note" text,
	"created_at" timestamp DEFAULT now() NOT NULL,
	"updated_at" timestamp DEFAULT now() NOT NULL
);
--> statement-breakpoint
ALTER TABLE "attendance_records" ADD CONSTRAINT "attendance_records_user_id_users_id_fk" FOREIGN KEY ("user_id") REFERENCES "public"."users"("id") ON DELETE cascade ON UPDATE no action;
--> statement-breakpoint
ALTER TABLE "attendance_records" ADD CONSTRAINT "attendance_records_course_id_courses_id_fk" FOREIGN KEY ("course_id") REFERENCES "public"."courses"("id") ON DELETE cascade ON UPDATE no action;
--> statement-breakpoint
CREATE UNIQUE INDEX "attendance_course_week_unique" ON "attendance_records" USING btree ("course_id","week_number");
