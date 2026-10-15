import { Router } from "express";
import { authController } from "./auth.controller";
import { authMiddleware } from "../../shared/middleware/auth.middleware";
import { validate } from "../../shared/middleware/validate.middleware";
import { authRateLimiter, passwordResetRateLimiter } from "../../shared/middleware/rateLimiter.middleware";
import { changePasswordSchema, forgotPasswordSchema, googleLoginSchema, loginSchema, refreshSchema, registerSchema, resetPasswordSchema, verifyEmailSchema } from "./auth.validator";

const router = Router();

// Authentication
router.post("/google", authRateLimiter, validate(googleLoginSchema), (req, res) =>
  authController.googleLogin(req, res)
);

router.post("/login", authRateLimiter, validate(loginSchema), (req, res) =>
  authController.emailPasswordLogin(req, res)
);

router.post("/register", authRateLimiter, validate(registerSchema), (req, res) =>
  authController.register(req, res)
);

// Token management
router.post("/refresh", validate(refreshSchema), (req, res) =>
  authController.refresh(req, res)
);

router.post("/logout", validate(refreshSchema), (req, res) =>
  authController.logout(req, res)
);

router.post(
  "/logout-all",
  authMiddleware,
  (req, res) => authController.logoutAll(req, res)
);

router.delete(
  "/devices/:tokenId",
  authMiddleware,
  (req, res) => authController.logoutDevice(req, res)
);

router.get(
  "/devices",
  authMiddleware,
  (req, res) => authController.getDevices(req, res)
);

// Email verification
router.post(
  "/verify-email/request",
  authMiddleware,
  passwordResetRateLimiter,
  (req, res) => authController.requestEmailVerification(req, res)
);

router.post("/verify-email", validate(verifyEmailSchema), (req, res) =>
  authController.verifyEmail(req, res)
);

// Password management
router.post("/forgot-password", passwordResetRateLimiter, validate(forgotPasswordSchema), (req, res) =>
  authController.forgotPassword(req, res)
);

router.post("/reset-password", passwordResetRateLimiter, validate(resetPasswordSchema), (req, res) =>
  authController.resetPassword(req, res)
);

router.post(
  "/change-password",
  authMiddleware,
  passwordResetRateLimiter,
  validate(changePasswordSchema),
  (req, res) => authController.changePassword(req, res)
);

// User info
router.get(
  "/me",
  authMiddleware,
  (req, res) => authController.me(req, res)
);

// Android ve web portalı, doğrulanmış öğrenci oturumuyla aynı silme işlemini kullanır.
router.delete(
  "/me",
  authMiddleware,
  (req, res) => authController.deleteAccount(req, res)
);

export default router;
