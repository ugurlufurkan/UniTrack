# UniTrack Admin Panel

Next.js 16 + better-auth + Drizzle ORM ile içerik yönetim paneli.
**UniTrack mobil backend'i ile AYNI Postgres veritabanını** kullanır — öğrenci
verisine (users, courses...) hiç dokunmaz, sadece bu görev için eklenen 4
tabloyu (announcements, faqs, tips, static_pages) yönetir.

Bu paket `npx next build` ile **gerçekten derlendi** (gerçek `better-auth`,
`drizzle-orm`, `next` paketleriyle, sahte bir DB bağlantı string'iyle) — kod
çalışır durumda teslim edildi.

## Kurulum

```bash
npm install
cp .env.example .env.local   # DATABASE_URL'i mobil backend'inizle AYNI yapın
```

`.env.local`:
- `DATABASE_URL`: **backend/.env'deki ile birebir aynı** olmalı (aynı Postgres).
- `BETTER_AUTH_SECRET`: `openssl rand -base64 32` ile üretin.
- `BETTER_AUTH_URL` / `NEXT_PUBLIC_BETTER_AUTH_URL`: panelin kendi adresi.

## Veritabanı

Bu panel, mobil backend'in `backend/src/db/schema.ts`'inde tanımladığı
`announcements/faqs/tips/static_pages` tablolarının **aynısını** kendi
`lib/db/schema.ts`'inde tanımlıyor (isim/kolon birebir), artı kendi
`admin_users/admin_sessions/admin_accounts/admin_verifications` tablolarını
(better-auth için, mobil `users` tablosuyla karışmasın diye ayrı).

En temiz yol: migration'ları SADECE backend tarafında çalıştırıp (asıl kaynak
schema.ts orada), admin panelde sadece `admin_*` tablolarını oluşturmak için
bir kere:

```bash
npm run db:push
```

## Admin hesabı oluşturma

Kayıt formu **yok** (kasıtlı — sadece siz/Mehmet gireceği için):

```bash
npm run create-admin -- "mehmet@ornek.com" "GucluBirSifre123" "Mehmet Doğan"
```

Aynı e-posta ile tekrar çalıştırırsanız şifreyi günceller.

## Çalıştırma

```bash
npm run dev      # http://localhost:3000
npm run build && npm start   # prod
```

## Sayfalar

- `/announcements` — Duyurular (tüm kullanıcılara gösterilir)
- `/faqs` — Sıkça Sorulan Sorular
- `/tips` — Dashboard'da dönen ipucu/motivasyon mesajları
- `/pages` — Statik sayfalar (Hakkımızda, KVKK, vb. — slug ile mobilde çekilir)
- `/deletion-requests` — Web veya uygulama üzerinden gelen hesap silme talepleri

Her sayfada satır-içi düzenleme var: değiştirip "Kaydet"e basmanız yeterli.
Mobil uygulama bu içerikleri `GET /api/v1/content/*` üzerinden okuyor (bkz.
backend paketindeki `modules/content`).

## Google Play için: girişsiz herkese açık sayfalar

Yukarıdaki `/pages`, `/deletion-requests` vb. hepsi login arkasında. Play
Console'un istediği (Gizlilik Politikası URL'i, hesap silme web sayfası) ise
GİRİŞSİZ erişilebilir sayfalar — bunun için ayrı, `(admin)` grubunun dışında
iki route var:

- `/legal/[slug]` — `/pages` ekranında oluşturduğun her `static_pages`
  satırını okunabilir bir HTML sayfası olarak gösterir. `npm run
  seed-legal-pages` ile `privacy` ve `terms` slug'larına TASLAK içerik
  eklenir; yayınlamadan önce `/pages` ekranından gerçek metinle
  güncelleyip bir avukata gözden geçirt.
- `/legal/delete-account` — Play Console'a hesap silme sayfası olarak
  verilecek URL. E-posta ile gelen talepler `/deletion-requests`
  ekranında listelenir; oradan "Tamamlandı" işaretlemek veriyi OTOMATİK
  SİLMİYOR — backend'de ilgili kullanıcının verisini silmek hâlâ elle
  yapılması gereken bir adım.

Play Console'da kullanacağın URL'ler (kendi domain'inle):
`https://SENIN-DOMAININ/legal/privacy` ve
`https://SENIN-DOMAININ/legal/delete-account`.

## Güvenlik notları

- `proxy.ts` (eski adıyla middleware) sadece cookie'nin VARLIĞINA bakan
  hızlı/iyimser bir kontrol; asıl yetki kontrolü her sayfada ve her server
  action'da `requireAdmin()` ile TEKRAR yapılıyor.
- `emailAndPassword.disableSignUp: true` — kimse kendine hesap açamaz,
  hesaplar sadece `create-admin` script'iyle açılır.
