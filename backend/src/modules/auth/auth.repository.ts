import { and, eq, isNull, gt, lt, sql } from "drizzle-orm";
import { db } from "../../db";
import { refreshTokens, users, pushSubscriptions } from "../../db/schema";
import { hashRefreshToken, hashRecoveryToken } from "../../lib/jwt";
import { AppError } from "../../shared/errors/AppError";

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
      .where(sql`lower(${users.email}) = ${email.trim().toLowerCase()}`);

    return result[0];
  }

  async findById(userId: string) {
    const result = await db.select().from(users).where(eq(users.id, userId));

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
      .where(eq(users.emailVerificationToken, hashRecoveryToken(token)));

    return result[0];
  }

  async findByPasswordResetToken(token: string) {
    const result = await db
      .select()
      .from(users)
      .where(eq(users.passwordResetToken, hashRecoveryToken(token)));

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

  async updateUser(
    userId: string,
    data: Partial<{
      googleId: string;
      password: string;
      isEmailVerified: boolean;
      emailVerificationToken: string | null;
      emailVerificationExpiresAt: Date | null;
      passwordResetToken: string | null;
      passwordResetExpiresAt: Date | null;
    }>,
  ) {
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

  async consumeEmailVerification(token: string) {
    const [user] = await db.update(users).set({ isEmailVerified: true, emailVerificationToken: null, emailVerificationExpiresAt: null, updatedAt: new Date() })
      .where(and(eq(users.emailVerificationToken, hashRecoveryToken(token)), gt(users.emailVerificationExpiresAt, new Date())))
      .returning({ id: users.id });
    return user;
  }

  async consumePasswordReset(userId: string, token: string, password: string) {
    return db.transaction(async tx => {
      await tx.select({ id: users.id }).from(users).where(eq(users.id, userId)).for("no key update");
      const [user] = await tx.update(users).set({ password, passwordResetToken: null, passwordResetExpiresAt: null, updatedAt: new Date() })
        .where(and(eq(users.id, userId), eq(users.passwordResetToken, hashRecoveryToken(token)), gt(users.passwordResetExpiresAt, new Date())))
        .returning({ id: users.id });
      if (user) await tx.update(refreshTokens).set({ revokedAt: new Date() }).where(eq(refreshTokens.userId, userId));
      return user;
    });
  }

  async changePasswordIfCurrent(userId: string, previousHash: string, password: string) {
    return db.transaction(async tx => {
      await tx.select({ id: users.id }).from(users).where(eq(users.id, userId)).for("no key update");
      const [user] = await tx.update(users).set({ password, passwordResetToken: null, passwordResetExpiresAt: null, updatedAt: new Date() })
        .where(and(eq(users.id, userId), eq(users.password, previousHash))).returning({ id: users.id });
      if (user) await tx.update(refreshTokens).set({ revokedAt: new Date() }).where(eq(refreshTokens.userId, userId));
      return user;
    });
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
    },
    expectedPassword?: string | null,
  ) {
    return db.transaction(async tx => {
      const [user] = await tx.select({ id: users.id, password: users.password }).from(users).where(eq(users.id, userId)).for("no key update");
      if (!user || (expectedPassword !== undefined && user.password !== expectedPassword)) throw AppError.unauthorized("Hesap bilgileri değişti. Lütfen tekrar giriş yap.");
      const [session] = await tx.insert(refreshTokens).values({
        userId,
        token: hashRefreshToken(token),
        expiresAt,
        deviceName: deviceInfo?.deviceName,
        deviceType: deviceInfo?.deviceType,
        userAgent: deviceInfo?.userAgent,
        ipAddress: deviceInfo?.ipAddress,
      }).returning({ id: refreshTokens.id });
      return session.id;
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
        replacedBy: refreshTokens.replacedBy,
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

  private async revokeChain(initialHash: string, ownerId?: string) {
    return db.transaction(async tx => {
      const [initial] = await tx.select({ userId: refreshTokens.userId }).from(refreshTokens).where(eq(refreshTokens.token, initialHash));
      if (!initial || (ownerId && initial.userId !== ownerId)) return 0;
      await tx.select({ id: users.id }).from(users).where(eq(users.id, initial.userId)).for("no key update");
      let current: string | null = initialHash;
      let owner = initial.userId;
      let count = 0;
      const visited = new Set<string>();
      while (current && !visited.has(current) && count < 128) {
        visited.add(current);
        const [row] = await tx.select().from(refreshTokens).where(and(
          eq(refreshTokens.token, current), owner ? eq(refreshTokens.userId, owner) : undefined,
        )).for("update");
        if (!row) break;
        owner = row.userId;
        await tx.update(refreshTokens).set({ revokedAt: row.revokedAt || new Date() }).where(eq(refreshTokens.id, row.id));
        current = row.replacedBy;
        count++;
      }
      if (current && count === 128 && owner) await tx.update(refreshTokens).set({ revokedAt: new Date() }).where(eq(refreshTokens.userId, owner));
      return count;
    });
  }

  async revokeRefreshToken(token: string) {
    return this.revokeChain(hashRefreshToken(token));
  }

  async revokeAllRefreshTokens(userId: string) {
    await db.transaction(async tx => {
      await tx.select({ id: users.id }).from(users).where(eq(users.id, userId)).for("no key update");
      await tx
        .update(refreshTokens)
        .set({ revokedAt: new Date() })
        .where(eq(refreshTokens.userId, userId));
    });
  }

  async rotateRefreshToken(
    userId: string,
    previous: string,
    next: string,
    expiresAt: Date,
    rotatedAt: Date,
    deviceInfo?: {
      deviceName: string;
      deviceType: string;
      userAgent?: string;
      ipAddress?: string;
    },
  ) {
    return db.transaction(async (tx) => {
      await tx.select({ id: users.id }).from(users).where(eq(users.id, userId)).for("no key update");
      const claimed = await tx
        .update(refreshTokens)
        .set({ revokedAt: rotatedAt, replacedBy: hashRefreshToken(next) })
        .where(
          and(
            eq(refreshTokens.token, hashRefreshToken(previous)),
            isNull(refreshTokens.revokedAt),
            gt(refreshTokens.expiresAt, new Date()),
          ),
        )
        .returning({ id: refreshTokens.id });
      if (!claimed.length) return false;
      const [replacement] = await tx
        .insert(refreshTokens)
        .values({
          userId,
          token: hashRefreshToken(next),
          expiresAt,
          ...deviceInfo,
        }).returning({ id: refreshTokens.id });
      await tx.update(pushSubscriptions).set({ tokenId: replacement.id }).where(eq(pushSubscriptions.tokenId, claimed[0].id));
      return replacement.id;
    });
  }

  async revokeRefreshTokenById(userId: string, tokenId: string) {
    const [row] = await db.select({ token: refreshTokens.token }).from(refreshTokens).where(and(eq(refreshTokens.id, tokenId), eq(refreshTokens.userId, userId)));
    return row ? this.revokeChain(row.token, userId) : 0;
  }

  async getUserRefreshTokens(userId: string) {
    const result = await db
      .select()
      .from(refreshTokens)
      .where(
        and(
          eq(refreshTokens.userId, userId),
          isNull(refreshTokens.revokedAt),
          gt(refreshTokens.expiresAt, new Date()),
        ),
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
