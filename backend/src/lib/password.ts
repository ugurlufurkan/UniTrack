import bcrypt from "bcryptjs";
import validator from "validator";

const SALT_ROUNDS = 12;

export interface PasswordStrengthResult {
  isValid: boolean;
  score: number;
  feedback: string[];
}

export function hashPassword(password: string): string {
  return bcrypt.hashSync(password, SALT_ROUNDS);
}

export function comparePassword(password: string, hash: string): boolean {
  return bcrypt.compareSync(password, hash);
}

export function validatePasswordStrength(password: string): PasswordStrengthResult {
  const feedback: string[] = [];
  let score = 0;

  if (password.length < 8) {
    feedback.push("Password must be at least 8 characters long");
  } else {
    score += 1;
  }

  if (!/[A-Z]/.test(password)) {
    feedback.push("Password must contain at least one uppercase letter");
  } else {
    score += 1;
  }

  if (!/[a-z]/.test(password)) {
    feedback.push("Password must contain at least one lowercase letter");
  } else {
    score += 1;
  }

  if (!/[0-9]/.test(password)) {
    feedback.push("Password must contain at least one number");
  } else {
    score += 1;
  }

  if (!/[^A-Za-z0-9]/.test(password)) {
    feedback.push("Password must contain at least one special character");
  } else {
    score += 1;
  }

  if (password.length > 12) {
    score += 1;
  }

  return {
    isValid: score >= 4,
    score,
    feedback,
  };
}

export function isValidEmail(email: string): boolean {
  return validator.isEmail(email);
}
