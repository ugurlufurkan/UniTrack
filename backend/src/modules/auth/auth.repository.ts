import { and, eq, isNull, gt, lt } from "drizzle-orm";
import { db } from "../../db";
import { refreshTokens, users } from "../../db/schema";
import { hashRefreshToken } from "../../lib/jwt";

class AuthRepository {
  async findByGoogleId(googleId: string) {
    const result = await db
      .select()
      .from(users)
      .where(eq(users.googleId, googleId));

    return result[0];
  }

  async findByEmail(email: string) {
    const result = await db
      .select()
      .from(users)
      .where(eq(users.email, email));

    return result[0];
  }

  async findById(userId: string) {
    const result = await db
      .select()
      .from(users)
      .where(eq(users.id, userId));

    return result[0];
  }

  // Google Play "hesap silme" şartı. `users` tablosuna bağlı HER ŞEY
  // (semesters, courses -> courseComponents/courseSchedule/attendanceRecords,
  // refreshTokens, events -> eventNotifications/eventChecklistItems)
  // schema.ts'de zaten `onDelete: "cascade"` ile tanımlı — tek satırlık bu
  // silme Postgres'in kendisi tarafından tüm ilişkili verilere yayılıyor.
  // auditLogs KASITLI OLARAK cascade değil (`onDelete: "set null"`) — denetim
  // kaydı, kime ait olduğu belirsizleşmiş halde saklanmaya devam ediyor.
  async deleteUser(userId: string) {
    await db.delete(users).where(eq(users.id, userId));
  }

  async findByEmailVerificationToken(token: string) {
    const result = await db
      .select()
      .from(users)
      .where(eq(users.emailVerificationToken, token));

    return result[0];
  }

  async findByPasswordResetToken(token: string) {
    const result = await db
      .select()
      .from(users)
      .where(eq(users.passwordResetToken, token));

    return result[0];
  }

  async createUser(data: {
    googleId?: string;
    email: string;
    password?: string;
    name: string;
    picture?: string;
  }) {
    const result = await db
      .insert(users)
      .values({
        googleId: data.googleId,
        email: data.email,
        password: data.password,
        name: data.name,
        picture: data.picture,
      })
      .returning();

    return result[0];
  }

  async updateUser(userId: string, data: Partial<{
    password: string;
    isEmailVerified: boolean;
    emailVerificationToken: string;
    emailVerificationExpiresAt: Date;
    passwordResetToken: string;
    passwordResetExpiresAt: Date;
  }>) {
    const result = await db
      .update(users)
      .set({
        ...data,
        updatedAt: new Date(),
      })
      .where(eq(users.id, userId))
      .returning();

    return result[0];
  }

  async saveRefreshToken(
    userId: string,
    token: string,
    expiresAt: Date,
    deviceInfo?: {
      deviceName?: string;
      deviceType?: string;
      userAgent?: string;
      ipAddress?: string;
    }
  ) {
    await db.insert(refreshTokens).values({
      userId,
      token: hashRefreshToken(token),
      expiresAt,
      deviceName: deviceInfo?.deviceName,
      deviceType: deviceInfo?.deviceType,
      userAgent: deviceInfo?.userAgent,
      ipAddress: deviceInfo?.ipAddress,
    });
  }

  // Returns the token row regardless of revoked/expired status — the caller
  // (auth.service.refresh) needs to see a revoked row to detect reuse.
  // Do NOT filter by isNull(revokedAt) here; that was the bug that made
  // reuse detection unreachable (a reused token always looked "not found").
  async findRefreshToken(token: string) {
    const result = await db
      .select({
        id: refreshTokens.id,
        token: refreshTokens.token,
        expiresAt: refreshTokens.expiresAt,
        revokedAt: refreshTokens.revokedAt,
        isReused: refreshTokens.isReused,
        user: users,
      })
      .from(refreshTokens)
      .innerJoin(users, eq(refreshTokens.userId, users.id))
      .where(eq(refreshTokens.token, hashRefreshToken(token)));

    return result[0];
  }

  async updateRefreshTokenLastUsed(tokenId: string) {
    await db
      .update(refreshTokens)
      .set({
        lastUsedAt: new Date(),
      })
      .where(eq(refreshTokens.id, tokenId));
  }

  async markRefreshTokenAsReused(token: string) {
    await db
      .update(refreshTokens)
      .set({
        isReused: true,
      })
      .where(eq(refreshTokens.token, hashRefreshToken(token)));
  }

  async revokeRefreshToken(
    token: string,
    replacedBy?: string
  ) {
    await db
      .update(refreshTokens)
      .set({
        revokedAt: new Date(),
        replacedBy: replacedBy ? hashRefreshToken(replacedBy) : replacedBy,
      })
      .where(eq(refreshTokens.token, hashRefreshToken(token)));
  }

  async revokeAllRefreshTokens(userId: string) {
    await db
      .update(refreshTokens)
      .set({
        revokedAt: new Date(),
      })
      .where(eq(refreshTokens.userId, userId));
  }

  async revokeRefreshTokenById(tokenId: string) {
    await db
      .update(refreshTokens)
      .set({
        revokedAt: new Date(),
      })
      .where(eq(refreshTokens.id, tokenId));
  }

  async getUserRefreshTokens(userId: string) {
    const result = await db
      .select()
      .from(refreshTokens)
      .where(
        and(
          eq(refreshTokens.userId, userId),
          isNull(refreshTokens.revokedAt),
          gt(refreshTokens.expiresAt, new Date())
        )
      )
      .orderBy(refreshTokens.createdAt);

    return result;
  }

  async cleanupExpiredRefreshTokens() {
    await db
      .delete(refreshTokens)
      .where(lt(refreshTokens.expiresAt, new Date()));
  }

  // ==========================
  // Compatibility Methods
  // ==========================

  async deleteRefreshToken(token: string) {
    await this.revokeRefreshToken(token);
  }

  async deleteAllRefreshTokens(userId: string) {
    await this.revokeAllRefreshTokens(userId);
  }
}

export const authRepository = new AuthRepository();