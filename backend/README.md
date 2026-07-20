# UniTrack Backend

Mobil uygulamanın ve admin panelin konuştuğu REST API.

**Teknolojiler:** Node.js, Express, TypeScript, Drizzle ORM, PostgreSQL (Neon), JWT tabanlı kimlik doğrulama.

## Kurulum

npm install
cp .env.example .env


`.env` içine doldurulması gerekenler:

- `DATABASE_URL` — PostgreSQL bağlantı adresi (Neon önerilir, ücretsiz plan yeterli)
- `PORT` — varsayılan 5000
- `JWT_SECRET`, `JWT_REFRESH_SECRET` — rastgele uzun string'ler
- `GOOGLE_CLIENT_ID` — Google ile giriş için

## Veritabanı

npm run db:generate # schema.ts değişikliklerinden migration dosyası üretir
npm run db:migrate # migration'ları veritabanına uygular


Not: `drizzle-kit migrate` bazı durumlarda migration'ı gerçekten uygulamadan
sessizce "tamamlandı" diyebiliyor (bilinen bir tuhaflık). Tabloların gerçekten
oluşup oluşmadığını doğrulamak için:

```sql
SELECT tablename FROM pg_tables WHERE schemaname = 'public' ORDER BY tablename;
```

Eksik tablo varsa, ilgili `drizzle/*.sql` dosyasını doğrudan veritabanına
uygulamak gerekebilir.

## Çalıştırma

npm run dev


`http://localhost:5000` üzerinde ayağa kalkar. API kök yolu: `/api/v1`.

## Modüller

`src/modules/` altında her biri kendi controller/service/repository/routes
dosyalarına sahip: `auth`, `semester`, `course`, `grade-scale`, `calendar`,
`attendance`, `task`, `transcript`, `gpa`, `statistics`, `dashboard`,
`settings` (tema/hedef GANO/sınav dönemi senkronu), `export` (veri dışa
aktarma), `content` (admin panelin yazdığı duyuru/SSS/ipucu/statik sayfaları
mobile salt-okunur sunar).
