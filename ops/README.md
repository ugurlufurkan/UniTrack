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
docker compose --env-file ops/.env -f ops/compose.yml up -d backend web ingress monitor
docker compose --env-file ops/.env -f ops/compose.yml ps
```

Caddy, doğru DNS ve açık 80/443 portlarıyla HTTPS sertifikalarını alıp yeniler. Web istemci IP başlığı ingress tarafından ezilir ve Next.js tarafından HMAC imzalanır. API ve web'in önüne başka bir CDN/proxy eklersen güven zincirini yeniden doğrula.

`APP_TIMEZONE`, API'deki “bugün” hesabını sunucunun saat diliminden bağımsız belirler; varsayılan `Europe/Istanbul` değeridir. IANA saat dilimi adını kullan. Bildirim worker'ında `PUSH_TIMEZONE` verilirse tekrarlanan hatırlatmalar için bu değer kullanılır; aksi halde uygulamanın saat dilimi geçerlidir.

Bu profil yeni yerel PostgreSQL oluşturur. Mevcut Neon/veritabanındaki öğrenci verilerini kendiliğinden taşımaz. Mevcut sistem için veritabanı bağlantısını uyarlayıp doğrulanmış yedek/geri dönüş planı kullan; boş veritabanına geçerek mevcut hesapları kaybetme.

## Yayın kabulü

`node ops/tests/production.integration.mjs --build`, geçici konteynerlerde gerçek PostgreSQL migration'larını, eşzamanlı işlemleri ve üretim web/API imajlarını doğrular. Sağlık kontrolü, imzalı gateway, kayıt ve geçici hesabın silinmesi kontrol edilir. GitHub Actions aynı testi çalıştırır. Bu test DNS/sertifika verilmesini, gerçek SMTP teslimatını veya Google hesap girişini kapsamaz.

- `/api/live` süreç canlılığını, `/api/health` veritabanını ve gerekli migration tablosunu kontrol eder. Hazır olmayan veya kapanan servis 503 döner.
- Kaynak ortamda `backend/` içinde `npm run check:production -- --smtp` kullanılabilir; SMTP bağlantısı doğrulanır, e-posta gönderilmez. Runtime imajında kaynak/test araçları bulunmaz.
- Gerçek Google hesabıyla giriş, e-posta doğrulama ve şifre sıfırlama teslimatı ayrıca kullanıcı kontrollü kabul testidir. Bu repo bu kontrollerin yapıldığını iddia etmez.
- `/legal/privacy` ve `/legal/terms` için `static_pages` kayıtlarını gerçek yayın metinleriyle doldur. Admin paneli kaldırılmıştır.
- Android release için `UNITRACK_API_URL` ve `UNITRACK_WEB_URL` Gradle özelliklerini gerçek HTTPS adreslerine ayarla ve keystore ile imzala.

Güncellemede önce yedek al, yeni imajları derle, migration'ı bir kez uygula ve sağlık kontrolünden sonra trafiği aç. Veritabanı migration'ının geri alınması otomatik değildir; önceki imajı kullanmak her şema değişikliğini geri çevirmez.

## Hata ve erişilebilirlik takibi

Her API isteğine `X-Request-ID` verilir ve loglara aynı kimlik yazılır. Monitor, web/API sağlığını dakikada bir denetler; üç ardışık başarısızlıkta olay üretir, değişmeyen arızayı 15 dakika içinde tekrarlamaz ve düzelmeyi bir kez bildirir. Durum yeniden başlatmalarda korunur. `docker compose --env-file ops/.env -f ops/compose.yml logs --since 1h backend monitor` ile incele.

Dış bildirim için kendi alıcının `ALERT_WEBHOOK_URL` değerini tanımlayıp `ENABLE_EXTERNAL_ALERTS=true` yap. Bu açık seçim olmadan dışarıya bildirim gönderilmez; olaylar loglara yazılır. Gövde `service`, `status`, `timestamp`, `text` alanlarını içerir. Webhook adaptörünü kendi alıcında doğrula; öğrenci verisi veya sır gönderilmez.

## Şifreli yedek ve geri yükleme provası

`age-keygen` ile bir kimlik oluştur; özel anahtarı sunucudan ayrı güvenli bir yerde tut, yalnızca herkese açık `age1...` alıcısını `AGE_RECIPIENT` olarak tanımla. Docker aracıyla üretmek için `ops/keys/` dizinini oluşturup Backup imajındaki `age-keygen -o /keys/backup.agekey` komutuna bu dizini bağlayabilirsin. Özel anahtar, `.env` ve yedekler Git tarafından dışlanır.

`ops/backups/` dizinini oluştur ve izinlerini 700 yap. `BACKUP_ENABLED=true` ayarlayıp aşağıdaki servisi aç:

```sh
docker compose --env-file ops/.env -f ops/compose.yml --profile backup up -d backup monitor
```

İlk yedek hemen, sonrakiler 24 saatte bir alınır. `pg_dump` çıktısı diske açık halde yazılmadan age ile şifrelenir; SHA-256 özeti oluşturulur. Monitor, son başarılı yedek 26 saati geçerse bunu da arıza olarak izler. Otomatik yedek silme yapılmaz; saklama süresini kapasiteye göre belirle ve şifreli dosyalarla checksum dosyalarını sunucu dışında da sakla.

Geri yükleme provasında `BACKUP_FILE` yalnızca dosya adı, `BACKUP_IDENTITY_FILE` özel anahtarın yolu olmalıdır:

```sh
BACKUP_FILE=unitrack-YYYYMMDDTHHMMSSZ-1.dump.age docker compose --env-file ops/.env -f ops/compose.yml --profile recovery run --rm restore-check
```

Komut checksum'u doğrular, şifreyi çözer ve yeni `unitrack_restore_check_*` veritabanına transaction içinde geri yükler. Ana tablolar sorgulanır, geçici veritabanı sonunda kaldırılır. Canlı `unitrack` veritabanına geri yükleme yapmaz. Gerçek kurtarmada önce bu provayı tamamla, servisi bakım moduna al ve operatör kontrolüyle hedefi değiştir; otomatik veri üzerine yazma eklenmemiştir.

`node ops/tests/backup.integration.mjs` gerçek PostgreSQL verisini şifreleyip geri yükleyerek kayıt sayılarını ve geçici veritabanının temizlendiğini doğrular. Bu test için Docker ve derlenmiş backend gerekir.

## Tarayıcı hatırlatmaları

İsteğe bağlıdır ve kullanıcı cihaz ayarlarından izin vermeden abonelik oluşturulmaz. HTTPS ve desteklenen bir tarayıcı gerekir; iOS'ta portalı ana ekrana yüklemek gerekebilir. FCM, Mozilla, Apple ve Windows bildirim uçları desteklenir.

`backend/` içinde `npm run push:keys`, eşleşen VAPID anahtarlarını Git tarafından dışlanan `ops/keys/vapid.env` dosyasına yazar; mevcut dosyanın üzerine yazmaz. Anahtarları `ops/.env` içindeki VAPID alanlarına aktar, dosya izinlerini 600 yap ve özel anahtarı sakla. API ile worker aynı anahtarları kullanmalıdır. Anahtar değişimi mevcut aboneliklerde kullanıcıdan yeniden etkinleştirme gerektirir.

`PUSH_ENABLED=true` ayarla; ardından:

```sh
docker compose --env-file ops/.env -f ops/compose.yml --profile push up -d --build backend push monitor
```

Worker kalıcı kuyruk, benzersiz teslimat kaydı ve PostgreSQL `SKIP LOCKED` ile birden çok kopyayı destekler. 30 saniyede bir tarar, son 5 dakikadaki hatırlatmaları yakalar; geçici hatalarda en fazla 3 deneme yapar ve 10 dakikayı geçen teslimatı bırakır. Tekrarlanan etkinliklerde `PUSH_TIMEZONE` saat dilimini kullanır; varsayılan İstanbul'dur. Başarılı gönderim sonrası süreç çökmesi tekrar teslimata neden olabilir; tarayıcı aynı teslimat etiketini kullanır. Bildirim zamanı ve teslimatı tarayıcı/işletim sistemi tarafından geciktirilebilir.

Abonelik cihazın aktif oturumuna bağlıdır; yenilemede devam eder, oturum iptalinde gönderim durur. Çıkışta yerel izin kaydı ve abonelik temizlenir. Worker sağlıklı değilse arayüz yeni abonelik açmaz; monitor bu servisi de izler. İzole testler gerçek kullanıcılara push göndermez; canlı vendor teslimatı yayın kabulünde ayrıca doğrulanmalıdır.
