CREATE TABLE "course_components" (
	"id" uuid PRIMARY KEY DEFAULT gen_random_uuid() NOT NULL,
	"course_id" uuid NOT NULL,
	"name" varchar(100) NOT NULL,
	"weight" real NOT NULL,
	"score" real,
	"sort_order" integer DEFAULT 0 NOT NULL,
	"created_at" timestamp DEFAULT now() NOT NULL
);
--> statement-breakpoint
ALTER TABLE "courses" ADD COLUMN "grade_scale" jsonb;--> statement-breakpoint
ALTER TABLE "users" ADD COLUMN "default_grade_scale" jsonb;--> statement-breakpoint
ALTER TABLE "course_components" ADD CONSTRAINT "course_components_course_id_courses_id_fk" FOREIGN KEY ("course_id") REFERENCES "public"."courses"("id") ON DELETE cascade ON UPDATE no action;--> statement-breakpoint
ALTER TABLE "courses" DROP COLUMN "midterm";--> statement-breakpoint
ALTER TABLE "courses" DROP COLUMN "final";--> statement-breakpoint
ALTER TABLE "courses" DROP COLUMN "makeup";