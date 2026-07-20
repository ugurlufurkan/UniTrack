import { Router } from "express";
import { authController } from "./auth.controller";
import { authMiddleware } from "../../shared/middleware/auth.middleware";

const router = Router();

// Authentication
router.post("/google", (req, res) =>
  authController.googleLogin(req, res)
);

router.post("/login", (req, res) =>
  authController.emailPasswordLogin(req, res)
);

router.post("/register", (req, res) =>
  authController.register(req, res)
);

// Token management
router.post("/refresh", (req, res) =>
  authController.refresh(req, res)
);

router.post("/logout", (req, res) =>
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
  (req, res) => authController.requestEmailVerification(req, res)
);

router.post("/verify-email", (req, res) =>
  authController.verifyEmail(req, res)
);

// Password management
router.post("/forgot-password", (req, res) =>
  authController.forgotPassword(req, res)
);

router.post("/reset-password", (req, res) =>
  authController.resetPassword(req, res)
);

router.post(
  "/change-password",
  authMiddleware,
  (req, res) => authController.changePassword(req, res)
);

// User info
router.get(
  "/me",
  authMiddleware,
  (req, res) => authController.me(req, res)
);

// Google Play "hesap silme" şartı — Android Ayarlar > Hesabımı Sil ve
// admin-panel/app/legal/delete-account bu adrese değil (o sadece talebi
// admin panelde kaydediyor), doğrudan bu uygulama-içi rotaya gidiyor.
router.delete(
  "/me",
  authMiddleware,
  (req, res) => authController.deleteAccount(req, res)
);

export default router;