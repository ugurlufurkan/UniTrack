import nodemailer from "nodemailer";
import { env } from "../config/env";

// Email transporter configuration
const transporter = nodemailer.createTransport({
  host: env.SMTP_HOST || "smtp.gmail.com",
  port: Number(env.SMTP_PORT) || 587,
  secure: env.SMTP_SECURE === "true",
  auth: {
    user: env.SMTP_USER,
    pass: env.SMTP_PASS,
  },
} as any);

export interface EmailOptions {
  to: string;
  subject: string;
  html: string;
  text?: string;
}

export async function sendEmail(options: EmailOptions): Promise<boolean> {
  try {
    // In development, log email instead of sending
    if (env.NODE_ENV !== "production") {
      console.log("[EMAIL MOCK]", {
        to: options.to,
        subject: options.subject,
        html: options.html,
      });
      return true;
    }

    await transporter.sendMail({
      from: env.SMTP_FROM || "noreply@unitrack.app",
      to: options.to,
      subject: options.subject,
      html: options.html,
      text: options.text,
    });

    return true;
  } catch (error) {
    console.error("Failed to send email:", error);
    return false;
  }
}

export function generateVerificationEmail(token: string): string {
  const verificationUrl = `${env.FRONTEND_URL}/verify-email?token=${token}`;
  
  return `
    <!DOCTYPE html>
    <html>
      <head>
        <meta charset="utf-8">
        <meta name="viewport" content="width=device-width, initial-scale=1.0">
        <title>Verify Your Email</title>
        <style>
          body { font-family: Arial, sans-serif; line-height: 1.6; color: #333; }
          .container { max-width: 600px; margin: 0 auto; padding: 20px; }
          .button { 
            display: inline-block; 
            padding: 12px 24px; 
            background-color: #007bff; 
            color: white; 
            text-decoration: none; 
            border-radius: 5px;
            margin: 20px 0;
          }
          .footer { margin-top: 30px; padding-top: 20px; border-top: 1px solid #eee; font-size: 12px; color: #666; }
        </style>
      </head>
      <body>
        <div class="container">
          <h2>Welcome to UniTrack!</h2>
          <p>Thank you for signing up. Please verify your email address to complete your registration.</p>
          <a href="${verificationUrl}" class="button">Verify Email</a>
          <p>Or copy and paste this link into your browser:</p>
          <p>${verificationUrl}</p>
          <p>This link will expire in 24 hours.</p>
          <div class="footer">
            <p>If you didn't create an account with UniTrack, please ignore this email.</p>
          </div>
        </div>
      </body>
    </html>
  `;
}

export function generatePasswordResetEmail(token: string): string {
  const resetUrl = `${env.FRONTEND_URL}/reset-password?token=${token}`;
  
  return `
    <!DOCTYPE html>
    <html>
      <head>
        <meta charset="utf-8">
        <meta name="viewport" content="width=device-width, initial-scale=1.0">
        <title>Reset Your Password</title>
        <style>
          body { font-family: Arial, sans-serif; line-height: 1.6; color: #333; }
          .container { max-width: 600px; margin: 0 auto; padding: 20px; }
          .button { 
            display: inline-block; 
            padding: 12px 24px; 
            background-color: #dc3545; 
            color: white; 
            text-decoration: none; 
            border-radius: 5px;
            margin: 20px 0;
          }
          .footer { margin-top: 30px; padding-top: 20px; border-top: 1px solid #eee; font-size: 12px; color: #666; }
        </style>
      </head>
      <body>
        <div class="container">
          <h2>Reset Your Password</h2>
          <p>We received a request to reset your password. Click the button below to create a new password.</p>
          <a href="${resetUrl}" class="button">Reset Password</a>
          <p>Or copy and paste this link into your browser:</p>
          <p>${resetUrl}</p>
          <p>This link will expire in 1 hour.</p>
          <div class="footer">
            <p>If you didn't request a password reset, please ignore this email.</p>
          </div>
        </div>
      </body>
    </html>
  `;
}

export async function sendVerificationEmail(email: string, token: string): Promise<boolean> {
  return sendEmail({
    to: email,
    subject: "Verify Your Email - UniTrack",
    html: generateVerificationEmail(token),
  });
}

export async function sendPasswordResetEmail(email: string, token: string): Promise<boolean> {
  return sendEmail({
    to: email,
    subject: "Reset Your Password - UniTrack",
    html: generatePasswordResetEmail(token),
  });
}
