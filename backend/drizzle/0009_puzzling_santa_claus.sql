CREATE TABLE "rate_limit_buckets" (
	"key" text PRIMARY KEY NOT NULL,
	"hits" integer NOT NULL,
	"reset_at" timestamp with time zone NOT NULL
);
