ALTER TABLE "refresh_tokens" ADD COLUMN "revoked_at" timestamp;--> statement-breakpoint
ALTER TABLE "refresh_tokens" ADD COLUMN "replaced_by" text;--> statement-breakpoint
ALTER TABLE "refresh_tokens" ADD CONSTRAINT "refresh_tokens_token_unique" UNIQUE("token");