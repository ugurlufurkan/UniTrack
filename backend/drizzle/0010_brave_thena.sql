CREATE TABLE "web_push_deliveries" (
	"id" uuid PRIMARY KEY DEFAULT gen_random_uuid() NOT NULL,
	"subscription_id" uuid NOT NULL,
	"event_id" uuid NOT NULL,
	"notification_id" uuid NOT NULL,
	"occurrence_at" timestamp with time zone NOT NULL,
	"due_at" timestamp with time zone NOT NULL,
	"attempts" integer DEFAULT 0 NOT NULL,
	"next_attempt_at" timestamp with time zone DEFAULT now() NOT NULL,
	"lease_until" timestamp with time zone,
	"sent_at" timestamp with time zone,
	"failed_at" timestamp with time zone
);
--> statement-breakpoint
CREATE TABLE "web_push_subscriptions" (
	"id" uuid PRIMARY KEY DEFAULT gen_random_uuid() NOT NULL,
	"user_id" uuid NOT NULL,
	"token_id" uuid NOT NULL,
	"endpoint" text NOT NULL,
	"p256dh" text NOT NULL,
	"auth" text NOT NULL,
	"created_at" timestamp with time zone DEFAULT now() NOT NULL,
	CONSTRAINT "web_push_subscriptions_endpoint_unique" UNIQUE("endpoint")
);
--> statement-breakpoint
CREATE TABLE "web_push_worker_state" (
	"id" text PRIMARY KEY NOT NULL,
	"last_seen" timestamp with time zone NOT NULL
);
--> statement-breakpoint
ALTER TABLE "web_push_deliveries" ADD CONSTRAINT "web_push_deliveries_subscription_id_web_push_subscriptions_id_fk" FOREIGN KEY ("subscription_id") REFERENCES "public"."web_push_subscriptions"("id") ON DELETE cascade ON UPDATE no action;--> statement-breakpoint
ALTER TABLE "web_push_deliveries" ADD CONSTRAINT "web_push_deliveries_event_id_events_id_fk" FOREIGN KEY ("event_id") REFERENCES "public"."events"("id") ON DELETE cascade ON UPDATE no action;--> statement-breakpoint
ALTER TABLE "web_push_deliveries" ADD CONSTRAINT "web_push_deliveries_notification_id_event_notifications_id_fk" FOREIGN KEY ("notification_id") REFERENCES "public"."event_notifications"("id") ON DELETE cascade ON UPDATE no action;--> statement-breakpoint
ALTER TABLE "web_push_subscriptions" ADD CONSTRAINT "web_push_subscriptions_user_id_users_id_fk" FOREIGN KEY ("user_id") REFERENCES "public"."users"("id") ON DELETE cascade ON UPDATE no action;--> statement-breakpoint
ALTER TABLE "web_push_subscriptions" ADD CONSTRAINT "web_push_subscriptions_token_id_refresh_tokens_id_fk" FOREIGN KEY ("token_id") REFERENCES "public"."refresh_tokens"("id") ON DELETE cascade ON UPDATE no action;--> statement-breakpoint
CREATE UNIQUE INDEX "web_push_delivery_unique" ON "web_push_deliveries" USING btree ("subscription_id","notification_id","due_at");--> statement-breakpoint
CREATE INDEX "web_push_pending_idx" ON "web_push_deliveries" USING btree ("next_attempt_at") WHERE "web_push_deliveries"."sent_at" is null and "web_push_deliveries"."failed_at" is null;--> statement-breakpoint
CREATE INDEX "web_push_session_idx" ON "web_push_subscriptions" USING btree ("token_id");--> statement-breakpoint
CREATE INDEX "web_push_user_idx" ON "web_push_subscriptions" USING btree ("user_id");