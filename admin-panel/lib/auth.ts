import { betterAuth } from "better-auth";
import { drizzleAdapter } from "better-auth/adapters/drizzle";

import { db } from "./db";
import { adminAccount, adminSession, adminUser, adminVerification } from "./db/schema";

/**
 * Sadece admin girişleri için. Kayıt (sign up) UI'dan KAPALI — hesaplar
 * `npm run create-admin` script'iyle (bkz. scripts/create-admin.ts) elle
 * açılıyor, çünkü "sadece siz/Mehmet" gibi 1-2 sabit admin olacak, herkese
 * açık bir kayıt formu istemiyoruz.
 */
export const auth = betterAuth({
  secret: process.env.BETTER_AUTH_SECRET,
  baseURL: process.env.BETTER_AUTH_URL,

  database: drizzleAdapter(db, {
    provider: "pg",
    schema: {
      user: adminUser,
      session: adminSession,
      account: adminAccount,
      verification: adminVerification,
    },
  }),

  emailAndPassword: {
    enabled: true,
    disableSignUp: true, // /login dışında hesap açılamaz
  },

  session: {
    expiresIn: 60 * 60 * 24 * 14, // 14 gün
  },
});
