import bcrypt from "bcryptjs";
import validator from "validator";

const SALT_ROUNDS = 12;

export interface PasswordStrengthResult {
  isValid: boolean;
  score: number;
  feedback: string[];
}

export async function hashPassword(password: string): Promise<string> {
  return bcrypt.hash(password, SALT_ROUNDS);
}

export async function comparePassword(password: string, hash: string): Promise<boolean> {
  return bcrypt.compare(password, hash);
}

export function validatePasswordStrength(password: string): PasswordStrengthResult {
  const feedback: string[] = [];
  let score = 0;

  if (password.length < 8) {
    feedback.push("Şifre en az 8 karakter olmalı");
  } else {
    score += 1;
  }

  if (!/[A-Z]/.test(password)) {
    feedback.push("En az bir büyük harf kullan");
  } else {
    score += 1;
  }

  if (!/[a-z]/.test(password)) {
    feedback.push("En az bir küçük harf kullan");
  } else {
    score += 1;
  }

  if (!/[0-9]/.test(password)) {
    feedback.push("En az bir rakam kullan");
  } else {
    score += 1;
  }

  if (!/[^A-Za-z0-9]/.test(password)) {
    feedback.push("En az bir özel karakter kullan");
  } else {
    score += 1;
  }

  if (password.length > 12) {
    score += 1;
  }
  if (Buffer.byteLength(password, 'utf8') > 72) feedback.push("Şifre UTF-8 olarak 72 baytı aşamaz");

  return {
    isValid: password.length >= 8 && Buffer.byteLength(password, 'utf8') <= 72 && score >= 4,
    score,
    feedback,
  };
}

export function isValidEmail(email: string): boolean {
  return validator.isEmail(email);
}
