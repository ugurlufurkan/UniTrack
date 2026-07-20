ALTER TABLE "courses" ADD COLUMN "average" real;--> statement-breakpoint
ALTER TABLE "courses" ADD COLUMN "letter_grade" varchar(2);--> statement-breakpoint
ALTER TABLE "courses" ADD COLUMN "grade_point" real;--> statement-breakpoint
ALTER TABLE "courses" ADD COLUMN "passed" boolean DEFAULT false;