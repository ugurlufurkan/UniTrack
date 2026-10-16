# UniTrack yayın kurulumu

Bu profil sağlayıcıdan bağımsız bir Linux Docker sunucusu içindir. Henüz bir domain/sunucu seçilmedi; bu dosyalar canlıya yayın yapıldığı anlamına gelmez. Windows yerel geliştirme için kök README'yi kullan.

## İlk kurulum

1. Web ve API için iki DNS kaydını sunucuya yönlendir. Sunucuda yalnızca SSH, 80 ve 443 portlarını aç; web, API ve PostgreSQL portlarını internete açma.
2. `cp ops/.env.example ops/.env` ile yapılandırmayı oluştur; `chmod 600 ops/.env` uygula. Domain, SMTP ve birbirinden farklı rastgele anahtarları doldur. Veritabanı şifresini URL içinde güvenli olacak rastgele hexadecimal değer olarak üret. Örnek domainleri kullanma.
3. Google Cloud Console'da web OAuth istemcisinin yetkili JavaScript kaynağını `https://WEB_DOMAIN` yap. Android paket/SHA bilgilerini aynı projede yapılandır. `GOOGLE_CLIENT_ID` web istemcisidir ve web derlemesine de aynı değer girilir. Değişirse web imajını yeniden derle.

Depo kökünde:

```sh
docker compose --env-file ops/.env -f ops/compose.yml config --quiet
docker compose --env-file ops/.env -f ops/compose.yml build
docker compose --env-file ops/.env -f ops/compose.yml up -d db
docker compose --env-file ops/.env -f ops/compose.yml run --rm migrate
docker compose --env-file ops/.env -f ops/compose.yml up -d backend web ingress
docker compose --env-file ops/.env -f ops/compose.yml ps
```

Caddy, doğru DNS ve açık 80/443 portlarıyla HTTPS sertifikalarını alıp yeniler. Web istemci IP başlığı ingress tarafından ezilir ve Next.js tarafından HMAC imzalanır. API ve web'in önüne başka bir CDN/proxy eklersen güven zincirini yeniden doğrula.

Bu profil yeni yerel PostgreSQL oluşturur. Mevcut Neon/veritabanındaki öğrenci verilerini kendiliğinden taşımaz. Mevcut sistem için veritabanı bağlantısını uyarlayıp doğrulanmış yedek/geri dönüş planı kullan; boş veritabanına geçerek mevcut hesapları kaybetme.

## Yayın kabulü

`node ops/tests/production.integration.mjs --build`, geçici konteynerlerde gerçek PostgreSQL migration'larını, eşzamanlı işlemleri ve üretim web/API imajlarını doğrular. Sağlık kontrolü, imzalı gateway, kayıt ve geçici hesabın silinmesi kontrol edilir. GitHub Actions aynı testi çalıştırır. Bu test DNS/sertifika verilmesini, gerçek SMTP teslimatını veya Google hesap girişini kapsamaz.

- `/api/live` süreç canlılığını, `/api/health` veritabanını ve gerekli migration tablosunu kontrol eder. Hazır olmayan veya kapanan servis 503 döner.
- Kaynak ortamda `backend/` içinde `npm run check:production -- --smtp` kullanılabilir; SMTP bağlantısı doğrulanır, e-posta gönderilmez. Runtime imajında kaynak/test araçları bulunmaz.
- Gerçek Google hesabıyla giriş, e-posta doğrulama ve şifre sıfırlama teslimatı ayrıca kullanıcı kontrollü kabul testidir. Bu repo bu kontrollerin yapıldığını iddia etmez.
- `/legal/privacy` ve `/legal/terms` için `static_pages` kayıtlarını gerçek yayın metinleriyle doldur. Admin paneli kaldırılmıştır.
- Android release için `UNITRACK_API_URL` ve `UNITRACK_WEB_URL` Gradle özelliklerini gerçek HTTPS adreslerine ayarla ve keystore ile imzala.

Güncellemede önce yedek al, yeni imajları derle, migration'ı bir kez uygula ve sağlık kontrolünden sonra trafiği aç. Veritabanı migration'ının geri alınması otomatik değildir; önceki imajı kullanmak her şema değişikliğini geri çevirmez.
