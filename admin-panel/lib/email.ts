import nodemailer from "nodemailer";

// Mobil backend'deki lib/email.ts ile AYNI SMTP deseni — kasıtlı olarak
// birbirinden bağımsız iki küçük dosya tutuyoruz (admin panel ve backend ayrı
// process/deploy'lar), ama istersen ikisine de aynı SMTP_* değerlerini
// (aynı gönderen e-posta hesabı) verebilirsin.
const transporter = nodemailer.createTransport({
  host: process.env.SMTP_HOST || "smtp.gmail.com",
  port: Number(process.env.SMTP_PORT) || 587,
  secure: process.env.SMTP_SECURE === "true",
  auth: {
    user: process.env.SMTP_USER,
    pass: process.env.SMTP_PASS,
  },
});

export async function sendEmail(options: { to: string; subject: string; html: string }) {
  // Geliştirme ortamında (SMTP_USER tanımsızsa) gerçekten göndermek yerine
  // konsola yazdır — böylece local'de test ederken yanlışlıkla gerçek bir
  // adrese mail atılmaz, ama link'i konsoldan kopyalayıp test edebilirsin.
  if (!process.env.SMTP_USER) {
    console.log("[EMAIL MOCK]", options);
    return true;
  }

  try {
    await transporter.sendMail({
      from: process.env.SMTP_FROM || "noreply@unitrack.app",
      to: options.to,
      subject: options.subject,
      html: options.html,
    });
    return true;
  } catch (error) {
    console.error("E-posta gönderilemedi:", error);
    return false;
  }
}

export function deletionConfirmationEmail(confirmUrl: string): string {
  return `
    <!DOCTYPE html>
    <html>
      <body style="font-family: Arial, sans-serif; line-height: 1.6; color: #1e293b;">
        <div style="max-width: 560px; margin: 0 auto; padding: 24px;">
          <h2>UniTrack hesap silme talebi</h2>
          <p>
            Bu e-posta adresi için bir hesap silme talebi alındı. Eğer bu talebi
            SEN göndermediysen, bu e-postayı görmezden gel — hiçbir şey silinmeyecek.
          </p>
          <p>
            Talebi onaylamak ve hesabınla ilişkili tüm verilerin (ders/not/GANO,
            devamsızlık, görev ve takvim kayıtları dahil) kalıcı olarak silinmesi
            için aşağıdaki bağlantıya tıkla:
          </p>
          <p style="margin: 24px 0;">
            <a href="${confirmUrl}" style="display:inline-block;padding:12px 24px;background:#dc2626;color:#fff;text-decoration:none;border-radius:6px;">
              Hesap Silme Talebimi Onayla
            </a>
          </p>
          <p style="font-size: 13px; color: #64748b;">
            Bağlantı çalışmıyorsa: ${confirmUrl}
          </p>
          <p style="font-size: 13px; color: #64748b;">
            Bu bağlantı 24 saat sonra geçersiz olur. Onaylandıktan sonra silme
            işlemi ekibimiz tarafından incelenip kısa süre içinde uygulanır.
          </p>
        </div>
      </body>
    </html>
  `;
}
