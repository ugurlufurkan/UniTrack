import { z } from "zod";

export const googleLoginSchema = z.object({
  idToken: z.string().min(1, "Google ID Token zorunludur"),
});

const email = z
  .string()
  .trim()
  .max(255)
  .email("Geçerli bir e-posta adresi gir.")
  .toLowerCase();
const password = z
  .string()
  .min(1, "Şifre zorunludur.")
  .max(72, "Şifre en fazla 72 karakter olabilir.");
const token = z.string().min(1, "Bağlantı geçersiz.").max(4096);
export const loginSchema = z.object({ email, password });
export const registerSchema = z.object({
  email,
  password: password.min(8),
  name: z.string().trim().min(2).max(100),
});
export const refreshSchema = z.object({ refreshToken: token });
export const forgotPasswordSchema = z.object({ email });
export const resetPasswordSchema = z.object({
  token,
  newPassword: password.min(8),
});
export const verifyEmailSchema = z.object({ token });
export const changePasswordSchema = z.object({
  currentPassword: password,
  newPassword: password.min(8),
});
