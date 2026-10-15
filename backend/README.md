# UniTrack Backend

Android uygulamasının ve web öğrenci portalının kullandığı ortak REST API. Express 5, TypeScript, Drizzle ORM, PostgreSQL ve JWT tabanlı kimlik doğrulama kullanır.

## Kurulum

Node.js 22 veya üzeri ve erişilebilir bir PostgreSQL veritabanı gerekir. Komutları `backend/` klasöründe çalıştır:

```powershell
npm ci
Copy-Item .env.example .env
```

### Ortam değişkenleri

| Değişken                                | Açıklama                                                                                                                                                                |
| --------------------------------------- | ----------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| `DATABASE_URL`                          | Zorunlu PostgreSQL bağlantı adresi.                                                                                                                                     |
| `JWT_SECRET`, `JWT_REFRESH_SECRET`      | Zorunlu, birbirinden farklı ve rastgele imzalama anahtarları.                                                                                                           |
| `GOOGLE_CLIENT_ID`                      | Mevcut başlangıç doğrulamasında zorunlu Google web OAuth istemci kimliği.                                                                                               |
| `PORT`                                  | Varsayılan `5000`.                                                                                                                                                      |
| `NODE_ENV`                              | Yerelde `development`, yayında `production`.                                                                                                                            |
| `FRONTEND_URL`                          | Şifre sıfırlama ve e-posta doğrulama bağlantılarında kullanılacak web adresi. Varsayılan `http://localhost:3000`.                                                       |
| `ALLOWED_ORIGINS`                       | Üretimde doğrudan tarayıcı erişimi için virgülle ayrılmış köken listesi.                                                                                                |
| `SMTP_HOST`, `SMTP_PORT`, `SMTP_SECURE` | Üretim e-postaları için SMTP sunucusu, portu ve TLS bağlantı ayarı.                                                                                                     |
| `SMTP_USER`, `SMTP_PASS`, `SMTP_FROM`   | SMTP kimlik bilgileri ve gönderen adresi.                                                                                                                               |
| `GEMINI_API_KEY`                        | İsteğe bağlı PDF transkript çözümleme anahtarı. Tanımlanırsa çıkarılan PDF metni Gemini hizmetine gönderilir; tanımlanmazsa yerel kural tabanlı çözümleyici kullanılır. |

İki JWT anahtarı için ayrı ayrı rastgele değer üret:

```powershell
node -e "console.log(require('crypto').randomBytes(64).toString('hex'))"
```

Gerçek `.env` dosyasını paylaşma veya commit etme. Google girişinde web'in `NEXT_PUBLIC_GOOGLE_CLIENT_ID` ve Android'in sunucu OAuth istemcisi `GOOGLE_CLIENT_ID` ile eşleşmelidir.

## Veritabanı

```powershell
npm run db:migrate
```

Bu komut `drizzle/` altındaki mevcut migration'ları `DATABASE_URL` ile belirtilen veritabanına uygular. Şema geliştirilirken yeni migration üretmek için `npm run db:generate` kullan; üretilen SQL'i inceleyip sürüm kontrolüne ekle. Yayın veritabanına uygulamadan önce yedek al ve migration geçmişini kontrol et.

Web portalı kendi veritabanını veya migration'larını kullanmaz. Eski admin tabloları bu geçişte otomatik olarak silinmez. Duyuru, SSS ve `static_pages` içerikleri mevcut veritabanından okunur; admin içerik düzenleme arayüzü kaldırılmıştır.

## Çalıştırma

```powershell
npm run dev
```

| Adres                              | Kullanım                            |
| ---------------------------------- | ----------------------------------- |
| `http://localhost:5000/api/v1`     | Sürümlenmiş REST API                |
| `http://localhost:5000/api/health` | API ve veritabanı bağlantı kontrolü |
| `http://localhost:5000/api/docs`   | Swagger API dokümanı                |

Üretimde:

```powershell
npm run build
npm start
```

Migration'lar sunucu başlarken otomatik çalıştırılmaz; yayın adımında ayrıca uygulanmalıdır. Docker imajı [Dockerfile](./Dockerfile) ile hazırlanabilir; migration aracı geliştirme bağımlılığı olduğu için migration adımını tam bağımlılıkların bulunduğu ortamda çalıştır.

## Modüller

`src/modules/` altında özellikler controller, service, repository ve routes katmanlarına ayrılır:

| Modüller                            | Sorumluluk                                                                        |
| ----------------------------------- | --------------------------------------------------------------------------------- |
| `auth`                              | Kayıt/giriş, Google, e-posta doğrulama, şifre sıfırlama, oturumlar ve hesap silme |
| `semester`, `course`, `grade-scale` | Dönemler, dersler, not bileşenleri ve skalalar                                    |
| `calendar`, `attendance`, `task`    | Etkinlikler, ders programı, devamsızlık, görevler ve alt görevler                 |
| `dashboard`, `gpa`, `statistics`    | Akademik özet ve hesaplamalar                                                     |
| `transcript`, `transcript-import`   | Transkript özeti, PDF önizleme ve onaylanan verilerin içe aktarılması             |
| `settings`, `export`                | Hesaba bağlı tercihler ve JSON veri indirme                                       |
| `content`                           | Duyuru, SSS, ipucu ve herkese açık yasal metinler                                 |

PDF içe aktarma en fazla 10 MB PDF kabul eder. Farklı üniversite biçimlerinde sonuç değişebilir; kullanıcı kaydetmeden önce önizlemeyi düzenleyip onaylar.

## Web ve e-posta bağlantısı

[Web kurulumundaki](../web/README.md) `API_URL`, bu API'nin `/api/v1` adresini göstermelidir. Backend'in `FRONTEND_URL` değeri ise web portalının adresidir. Next.js, öğrenci isteklerini sunucu üzerinden iletir; öğrenci token'ları backend tarafından doğrulanır.

`NODE_ENV=production` olduğunda e-postalar SMTP ile gönderilir. Diğer ortamlarda e-posta içeriği yerel konsola yazılır; geliştirme SMTP teslimatını doğrulamaz.

## Kontroller

Migration geçmişi ayrıca PGlite içindeki PostgreSQL motorunda boş veritabanına uygulanır. İkinci çalıştırmanın güvenli olduğu, tablolar, ilişkiler ve varsayılan değerler otomatik doğrulanır. Bu kontrol mevcut öğrenci veritabanına bağlanmaz. `0006` içindeki yinelenen takvim komutları temizlenmiştir; migration sırası ve daha önce uygulanmış öğrenci verileri korunur.

```powershell
npm test
npm audit
```

`npm test` önce TypeScript derlemesini, ardından izole regresyon testlerini çalıştırır. Testler PostgreSQL'e bağlanmaz. Giriş doğrulaması, tarih dönüşümleri, not hesapları, saat aralıkları, şifre uzunluğu, Google hesap eşleme, token benzersizliği ve tek kullanımlık bağlantıların tüketilmesi kontrol edilir.

Çalışan yerel API ve yapılandırılmış PostgreSQL ile kabul kontrolü:

```powershell
node scripts/smoke-api.mjs http://127.0.0.1:5000/api/v1
```

Bu komut yalnızca açıkça verilen yerel API adresini kabul eder. Bağlı veritabanında geçici bir öğrenci hesabı oluşturur; dönem, ders, not, ayar, takvim, devamsızlık, görev, dışa aktarma ve oturum işlemlerini doğrular. Sonunda hesabı ilişkili verileriyle siler. Google OAuth ve gerçek SMTP teslimatı ayrıca test edilmelidir.

`drizzle-kit` geliştirme bağımlılığıdır. Eski yardımcı yükleyicisinin `esbuild` bağımlılığı `0.25.12` sürümüne sabitlenmiştir; `tsx` kendi uyumlu sürümünü kullanır.
