import { verifyGoogleToken } from "../../lib/google";
import {
  generateAccessToken,
  generateRefreshToken,
  verifyRefreshToken,
} from "../../lib/jwt";
import { authRepository } from "./auth.repository";
import { AppError } from "../../shared/errors/AppError";
import { createAuditLog, extractDeviceInfo, detectDeviceType, generateDeviceName } from "../../lib/audit";
import { hashPassword, comparePassword, validatePasswordStrength, isValidEmail } from "../../lib/password";
import { sendVerificationEmail, sendPasswordResetEmail } from "../../lib/email";
import { Request } from "express";
import { randomBytes } from "crypto";

class AuthService {
  private extractDeviceInfo(req: Request) {
    const deviceInfo = extractDeviceInfo(req);
    return {
      deviceName: generateDeviceName(deviceInfo.userAgent),
      deviceType: detectDeviceType(deviceInfo.userAgent),
      userAgent: deviceInfo.userAgent,
      ipAddress: deviceInfo.ipAddress,
    };
  }

  async googleLogin(idToken: string, req?: Request) {
    if (!idToken) {
      throw AppError.badRequest("idToken zorunludur.");
    }

    let payload;
    try {
      payload = await verifyGoogleToken(idToken);
    } catch (err) {
      throw AppError.unauthorized("Google token doğrulanamadı.");
    }

    if (!payload) {
      throw AppError.unauthorized("Google doğrulanamadı.");
    }

    let user = await authRepository.findByGoogleId(payload.sub);

    if (!user) {
      user = await authRepository.createUser({
        googleId: payload.sub,
        email: payload.email!,
        name: payload.name!,
        picture: payload.picture,
      });
    }

    const deviceInfo = req ? this.extractDeviceInfo(req) : undefined;

    const accessToken = generateAccessToken({
      userId: user.id,
      email: user.email,
    });

    const refreshToken = generateRefreshToken({
      userId: user.id,
    });

    await authRepository.saveRefreshToken(
      user.id,
      refreshToken,
      new Date(Date.now() + 1000 * 60 * 60 * 24 * 30),
      deviceInfo
    );

    await createAuditLog({
      userId: user.id,
      action: "LOGIN",
      entity: "USER",
      entityId: user.id,
      method: req?.method,
      path: req?.path,
      userAgent: deviceInfo?.userAgent,
      ipAddress: deviceInfo?.ipAddress,
    });

    return {
      success: true,
      message: "Giriş başarılı.",
      user,
      accessToken,
      refreshToken,
    };
  }

  async emailPasswordLogin(email: string, password: string, req?: Request) {
    if (!email || !password) {
      throw AppError.badRequest("Email ve şifre zorunludur.");
    }

    if (!isValidEmail(email)) {
      throw AppError.badRequest("Geçersiz email formatı.");
    }

    const deviceInfo = req ? this.extractDeviceInfo(req) : undefined;

    const user = await authRepository.findByEmail(email);

    if (!user || !user.password) {
      await createAuditLog({
        action: "LOGIN_FAILED",
        entity: "USER",
        method: req?.method,
        path: req?.path,
        userAgent: deviceInfo?.userAgent,
        ipAddress: deviceInfo?.ipAddress,
        metadata: { email, reason: "user_not_found" },
      });
      throw AppError.unauthorized("Geçersiz email veya şifre.");
    }

    const isPasswordValid = comparePassword(password, user.password);

    if (!isPasswordValid) {
      await createAuditLog({
        userId: user.id,
        action: "LOGIN_FAILED",
        entity: "USER",
        entityId: user.id,
        method: req?.method,
        path: req?.path,
        userAgent: deviceInfo?.userAgent,
        ipAddress: deviceInfo?.ipAddress,
        metadata: { email, reason: "invalid_password" },
      });
      throw AppError.unauthorized("Geçersiz email veya şifre.");
    }

    const accessToken = generateAccessToken({
      userId: user.id,
      email: user.email,
    });

    const refreshToken = generateRefreshToken({
      userId: user.id,
    });

    await authRepository.saveRefreshToken(
      user.id,
      refreshToken,
      new Date(Date.now() + 1000 * 60 * 60 * 24 * 30),
      deviceInfo
    );

    await createAuditLog({
      userId: user.id,
      action: "LOGIN",
      entity: "USER",
      entityId: user.id,
      method: req?.method,
      path: req?.path,
      userAgent: deviceInfo?.userAgent,
      ipAddress: deviceInfo?.ipAddress,
    });

    return {
      success: true,
      message: "Giriş başarılı.",
      user,
      accessToken,
      refreshToken,
    };
  }

  async register(data: {
    email: string;
    password: string;
    name: string;
  }, req?: Request) {
    const { email, password, name } = data;

    if (!email || !password || !name) {
      throw AppError.badRequest("Email, şifre ve isim zorunludur.");
    }

    if (!isValidEmail(email)) {
      throw AppError.badRequest("Geçersiz email formatı.");
    }

    const passwordStrength = validatePasswordStrength(password);
    if (!passwordStrength.isValid) {
      throw AppError.badRequest(
        `Şifre yetersiz: ${passwordStrength.feedback.join(", ")}`
      );
    }

    const existingUser = await authRepository.findByEmail(email);
    if (existingUser) {
      throw AppError.badRequest("Bu email zaten kayıtlı.");
    }

    const hashedPassword = hashPassword(password);

    const user = await authRepository.createUser({
      email,
      password: hashedPassword,
      name,
    });

    const deviceInfo = req ? this.extractDeviceInfo(req) : undefined;

    const accessToken = generateAccessToken({
      userId: user.id,
      email: user.email,
    });

    const refreshToken = generateRefreshToken({
      userId: user.id,
    });

    await authRepository.saveRefreshToken(
      user.id,
      refreshToken,
      new Date(Date.now() + 1000 * 60 * 60 * 24 * 30),
      deviceInfo
    );

    await createAuditLog({
      userId: user.id,
      action: "REGISTER",
      entity: "USER",
      entityId: user.id,
      method: req?.method,
      path: req?.path,
      userAgent: deviceInfo?.userAgent,
      ipAddress: deviceInfo?.ipAddress,
    });

    return {
      success: true,
      message: "Kayıt başarılı.",
      user,
      accessToken,
      refreshToken,
    };
  }

  async refresh(refreshToken: string, req?: Request) {
    try {
      verifyRefreshToken(refreshToken);
    } catch (err) {
      throw AppError.unauthorized("Refresh token geçersiz veya süresi dolmuş.");
    }

    const token = await authRepository.findRefreshToken(refreshToken);

    if (!token) {
      throw AppError.unauthorized("Refresh token geçersiz.");
    }

    // Reuse detection: a refresh token is single-use. If this row is already
    // revoked, it means it was already rotated away (or manually revoked) —
    // someone is presenting a token that shouldn't exist anymore. That's
    // either a replay of a stolen token or a rotated-out client retrying,
    // and in both cases the safe response is to kill every session for
    // this user rather than silently reject just this one request.
    if (token.revokedAt) {
      await authRepository.markRefreshTokenAsReused(refreshToken);
      await authRepository.revokeAllRefreshTokens(token.user.id);
      await createAuditLog({
        userId: token.user.id,
        action: "TOKEN_REUSE_DETECTED",
        entity: "REFRESH_TOKEN",
        entityId: token.id,
        method: req?.method,
        path: req?.path,
      });
      throw AppError.unauthorized("Güvenlik nedeniyle tüm oturumlar sonlandırıldı.");
    }

    if (token.expiresAt.getTime() < Date.now()) {
      throw AppError.unauthorized("Refresh token süresi dolmuş.");
    }

    // Refresh token rotation
    const deviceInfo = req ? this.extractDeviceInfo(req) : undefined;

    const newAccessToken = generateAccessToken({
      userId: token.user.id,
      email: token.user.email,
    });

    const newRefreshToken = generateRefreshToken({
      userId: token.user.id,
    });

    // Revoke old token and mark as replaced
    await authRepository.revokeRefreshToken(refreshToken, newRefreshToken);

    // Save new token
    await authRepository.saveRefreshToken(
      token.user.id,
      newRefreshToken,
      new Date(Date.now() + 1000 * 60 * 60 * 24 * 30),
      deviceInfo
    );

    await createAuditLog({
      userId: token.user.id,
      action: "TOKEN_REFRESH",
      entity: "REFRESH_TOKEN",
      entityId: token.id,
      method: req?.method,
      path: req?.path,
      userAgent: deviceInfo?.userAgent,
      ipAddress: deviceInfo?.ipAddress,
    });

    return {
      success: true,
      accessToken: newAccessToken,
      refreshToken: newRefreshToken,
    };
  }

  async logout(refreshToken: string, req?: Request) {
    const token = await authRepository.findRefreshToken(refreshToken);

    if (token) {
      await authRepository.revokeRefreshToken(refreshToken);

      await createAuditLog({
        userId: token.user.id,
        action: "LOGOUT",
        entity: "USER",
        entityId: token.user.id,
        method: req?.method,
        path: req?.path,
      });
    }

    return {
      success: true,
    };
  }

  // Google Play "hesap silme" şartı: hesabı ve tüm ilişkili verilerini
  // (ders/dönem/not, devamsızlık, takvim/görev, refresh token'lar) kalıcı
  // olarak siler. Denetim kaydı SİLMEDEN ÖNCE yazılıyor — audit_logs.user_id
  // "set null" cascade'i olduğu için, kullanıcı silindikten sonra bu satırı
  // o kullanıcıya bağlayamayız.
  async deleteAccount(userId: string, req?: Request) {
    const user = await authRepository.findById(userId);

    if (!user) {
      throw AppError.notFound("Kullanıcı bulunamadı.");
    }

    await createAuditLog({
      userId: user.id,
      action: "ACCOUNT_DELETED",
      entity: "USER",
      entityId: user.id,
      method: req?.method,
      path: req?.path,
    });

    await authRepository.deleteUser(userId);

    return {
      success: true,
    };
  }

  async logoutAll(userId: string, req?: Request) {
    await authRepository.revokeAllRefreshTokens(userId);

    await createAuditLog({
      userId,
      action: "LOGOUT_ALL",
      entity: "USER",
      entityId: userId,
      method: req?.method,
      path: req?.path,
    });

    return {
      success: true,
    };
  }

  async logoutDevice(userId: string, tokenId: string, req?: Request) {
    await authRepository.revokeRefreshTokenById(tokenId);

    await createAuditLog({
      userId,
      action: "LOGOUT_DEVICE",
      entity: "REFRESH_TOKEN",
      entityId: tokenId,
      method: req?.method,
      path: req?.path,
    });

    return {
      success: true,
    };
  }

  async getDevices(userId: string) {
    const tokens = await authRepository.getUserRefreshTokens(userId);

    return {
      success: true,
      devices: tokens.map((token) => ({
        id: token.id,
        deviceName: token.deviceName || "Unknown Device",
        deviceType: token.deviceType || "unknown",
        lastUsedAt: token.lastUsedAt,
        createdAt: token.createdAt,
        expiresAt: token.expiresAt,
      })),
    };
  }

  async requestEmailVerification(userId: string) {
    const user = await authRepository.findById(userId);

    if (!user) {
      throw AppError.notFound("Kullanıcı bulunamadı.");
    }

    if (user.isEmailVerified) {
      throw AppError.badRequest("Email zaten doğrulanmış.");
    }

    const token = randomBytes(32).toString("hex");
    const expiresAt = new Date(Date.now() + 1000 * 60 * 60 * 24); // 24 hours

    await authRepository.updateUser(userId, {
      emailVerificationToken: token,
      emailVerificationExpiresAt: expiresAt,
    });

    await sendVerificationEmail(user.email, token);

    await createAuditLog({
      userId,
      action: "EMAIL_VERIFICATION_REQUESTED",
      entity: "USER",
      entityId: userId,
    });

    return {
      success: true,
      message: "Doğrulama emaili gönderildi.",
    };
  }

  async verifyEmail(token: string) {
    const user = await authRepository.findByEmailVerificationToken(token);

    if (!user) {
      throw AppError.badRequest("Geçersiz veya süresi dolmuş token.");
    }

    if (user.emailVerificationExpiresAt && user.emailVerificationExpiresAt < new Date()) {
      throw AppError.badRequest("Token süresi dolmuş.");
    }

    await authRepository.updateUser(user.id, {
      isEmailVerified: true,
      emailVerificationToken: undefined,
      emailVerificationExpiresAt: undefined,
    });

    await createAuditLog({
      userId: user.id,
      action: "EMAIL_VERIFIED",
      entity: "USER",
      entityId: user.id,
    });

    return {
      success: true,
      message: "Email başarıyla doğrulandı.",
    };
  }

  async forgotPassword(email: string) {
    const user = await authRepository.findByEmail(email);

    if (!user) {
      // Don't reveal if email exists
      return {
        success: true,
        message: "Eğer email kayıtlıysa, şifre sıfırlama linki gönderilecektir.",
      };
    }

    const token = randomBytes(32).toString("hex");
    const expiresAt = new Date(Date.now() + 1000 * 60 * 60); // 1 hour

    await authRepository.updateUser(user.id, {
      passwordResetToken: token,
      passwordResetExpiresAt: expiresAt,
    });

    await sendPasswordResetEmail(user.email, token);

    await createAuditLog({
      userId: user.id,
      action: "PASSWORD_RESET_REQUESTED",
      entity: "USER",
      entityId: user.id,
    });

    return {
      success: true,
      message: "Eğer email kayıtlıysa, şifre sıfırlama linki gönderilecektir.",
    };
  }

  async resetPassword(token: string, newPassword: string) {
    const user = await authRepository.findByPasswordResetToken(token);

    if (!user) {
      throw AppError.badRequest("Geçersiz veya süresi dolmuş token.");
    }

    if (user.passwordResetExpiresAt && user.passwordResetExpiresAt < new Date()) {
      throw AppError.badRequest("Token süresi dolmuş.");
    }

    const passwordStrength = validatePasswordStrength(newPassword);
    if (!passwordStrength.isValid) {
      throw AppError.badRequest(
        `Şifre yetersiz: ${passwordStrength.feedback.join(", ")}`
      );
    }

    const hashedPassword = hashPassword(newPassword);

    await authRepository.updateUser(user.id, {
      password: hashedPassword,
      passwordResetToken: undefined,
      passwordResetExpiresAt: undefined,
    });

    // Revoke all refresh tokens for security
    await authRepository.revokeAllRefreshTokens(user.id);

    await createAuditLog({
      userId: user.id,
      action: "PASSWORD_RESET",
      entity: "USER",
      entityId: user.id,
    });

    return {
      success: true,
      message: "Şifre başarıyla sıfırlandı. Lütfen tekrar giriş yapın.",
    };
  }

  async changePassword(userId: string, currentPassword: string, newPassword: string) {
    const user = await authRepository.findById(userId);

    if (!user || !user.password) {
      throw AppError.badRequest("Kullanıcı bulunamadı veya şifre yok.");
    }

    const isPasswordValid = comparePassword(currentPassword, user.password);

    if (!isPasswordValid) {
      throw AppError.unauthorized("Mevcut şifre hatalı.");
    }

    const passwordStrength = validatePasswordStrength(newPassword);
    if (!passwordStrength.isValid) {
      throw AppError.badRequest(
        `Şifre yetersiz: ${passwordStrength.feedback.join(", ")}`
      );
    }

    const hashedPassword = hashPassword(newPassword);

    await authRepository.updateUser(userId, {
      password: hashedPassword,
    });

    // Revoke all refresh tokens for security
    await authRepository.revokeAllRefreshTokens(userId);

    await createAuditLog({
      userId,
      action: "PASSWORD_CHANGED",
      entity: "USER",
      entityId: userId,
    });

    return {
      success: true,
      message: "Şifre başarıyla değiştirildi. Lütfen tekrar giriş yapın.",
    };
  }
}

export const authService = new AuthService();