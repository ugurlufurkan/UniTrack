/**
 * `static_pages` tablosuna Gizlilik Politikası ve Kullanım Şartları için
 * TASLAK içerik ekler (yoksa). Bu içerik bir hukuk metni DEĞİL — UniTrack'in
 * gerçekte topladığı verilere (Google girişi, ad/soyad, e-posta, ders/not/
 * GANO/devamsızlık/görev/takvim kayıtları) göre yazılmış bir başlangıç
 * noktası. Play Console'a göndermeden önce mutlaka bir avukat/uzman ile
 * gözden geçirilmeli ve gerçek şirket/iletişim bilgileriyle güncellenmeli.
 *
 * Kullanım:
 *   npx tsx scripts/seed-legal-pages.ts
 */
import "dotenv/config";
import { eq } from "drizzle-orm";

import { db } from "../lib/db";
import { staticPages } from "../lib/db/schema";

const PRIVACY_CONTENT = `## Taslak — yayınlamadan önce gözden geçirilmeli

Bu metin UniTrack'in topladığı verilere göre otomatik hazırlanmış bir
başlangıç noktasıdır, hukuki tavsiye değildir.

## Topladığımız veriler

- Google ile giriş yaptığında: ad, soyad, e-posta adresi.
- Kendi girdiğin akademik veriler: dönem, ders, not, GANO hesaplamaları,
  devamsızlık kayıtları, görev/ödev ve takvim etkinlikleri.
- Uygulamanın çalışması için gerekli teknik veriler (oturum token'ları,
  bildirim tercihleri).

## Verileri nasıl kullanıyoruz

Bu veriler yalnızca uygulamanın temel işlevlerini (not/GANO takibi,
devamsızlık hesaplama, hatırlatma bildirimleri) sağlamak için kullanılır.
Verilerin satışı yapılmaz.

## Verilerini silme

Hesabını ve ilişkili verilerini istediğin zaman silebilirsin:

- Uygulama içinde: Ayarlar → Hesabımı Sil.
- Uygulama telefonunda kurulu değilse: [Hesap Silme Talebi](/legal/delete-account)
  sayfasından e-posta adresinle talep gönderebilirsin.

## İletişim

Sorularınız için: [furkanugurlu6806@gmail.com](mailto:furkanugurlu6806@gmail.com)`;

const TERMS_CONTENT = `## Taslak — yayınlamadan önce gözden geçirilmeli

## Kullanım şartları

UniTrack'i kullanarak, uygulamayı kişisel akademik takip amacıyla
kullanmayı ve girdiğin verilerin doğruluğundan kendinin sorumlu olduğunu
kabul edersin. GANO/not hesaplamaları bilgilendirme amaçlıdır; resmi
transkript yerine geçmez.

## Hesap

Hesabını Google ile oluşturursun. Hesabını istediğin zaman
[Hesap Silme Talebi](/legal/delete-account) sayfasından veya uygulama içi
Ayarlar → Hesabımı Sil ile kapatabilirsin.

## İletişim

Sorularınız için: [furkanugurlu6806@gmail.com](mailto:furkanugurlu6806@gmail.com)`;

async function upsert(slug: string, title: string, content: string) {
  const existing = await db.query.staticPages.findFirst({ where: eq(staticPages.slug, slug) });

  if (existing) {
    console.log(`"${slug}" zaten var, atlanıyor (elle düzenlemek için /pages ekranını kullan).`);
    return;
  }

  await db.insert(staticPages).values({ slug, title, content });
  console.log(`"${slug}" taslak içerikle oluşturuldu.`);
}

async function main() {
  await upsert("privacy", "Gizlilik Politikası", PRIVACY_CONTENT);
  await upsert("terms", "Kullanım Şartları", TERMS_CONTENT);
}

main()
  .then(() => process.exit(0))
  .catch((err) => {
    console.error(err);
    process.exit(1);
  });
