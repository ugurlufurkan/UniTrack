# UniTrack

Üniversite öğrencileri için ders, not, devamsızlık ve görev takibi yapan bir mobil uygulama. Android istemci, bir REST API backend'i ve bu ikisini besleyen bir içerik yönetim paneli olmak üzere üç parçadan oluşur.

## Proje yapısı

| Klasör | Ne işe yarar | Teknoloji |
|---|---|---|
| [`android/`](./android) | Öğrencinin kullandığı mobil uygulama | Kotlin, Jetpack Compose, Hilt, Retrofit |
| [`backend/`](./backend) | Mobil uygulamanın konuştuğu REST API | Node.js, Express, Drizzle ORM, PostgreSQL |
| [`admin-panel/`](./admin-panel) | Duyuru/SSS/ipucu/statik sayfa içeriğini APK güncellemeden yönetme paneli | Next.js, better-auth, Drizzle ORM |

Backend ve admin panel **aynı PostgreSQL veritabanını** paylaşır (şu an [Neon](https://neon.tech) üzerinde barındırılıyor). Admin panel içerik yazar, backend onu mobil uygulamaya salt-okunur olarak sunar (`GET /api/v1/content/*`).

## Mimari

- **Android** uygulaması REST üzerinden **Backend**'e istek atar.
- **Admin Panel** de aynı **Backend**'in arkasındaki veritabanına (Neon/PostgreSQL) doğrudan Drizzle ile yazar.
- Backend, admin panelin yazdığı içeriği (duyuru, SSS, ipucu, statik sayfa) `GET /api/v1/content/*` üzerinden Android'e salt-okunur sunar.
- Üçü de aynı PostgreSQL veritabanını (Neon) paylaşır; tek doğru veri kaynağı budur.

## Başlıca özellikler

- Dönem / ders / not takibi, GANO hesaplama ve simülasyonu
- Devamsızlık takibi, ders programı, takvim ve hatırlatıcılar
- Görev (task) yönetimi
- Transkript PDF çıktısı
- Hesap verilerini JSON olarak dışa aktarma ("verilerimi indir")
- Tema / hedef GANO / sınav dönemi ayarlarının hesaba bağlı senkronizasyonu (telefon değişince kaybolmaz)
- Admin panelden duyuru, SSS, ipucu ve statik sayfa (Hakkımızda, KVKK vb.) yönetimi

## Kurulum

Her alt klasörün kendi README'sinde detaylı kurulum adımları var:

1. [`backend/README.md`](./backend/README.md) — veritabanı, `.env`, migration
2. [`admin-panel/README.md`](./admin-panel/README.md) — admin panel kurulumu, admin hesabı açma
3. [`android/README.md`](./android/README.md) — Android Studio, emülatör/gerçek cihaz

Genel sıra: **önce backend, sonra admin panel, en son Android.**
