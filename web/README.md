# UniTrack Web

Next.js 16 / React 19 ile öğrenci portalı. Android ile aynı kullanıcı hesabını ve aynı API'yi kullanır. Admin paneli veya web'e özel veritabanı bağlantısı yoktur.

[Proje özeti](../README.md) · [Backend kurulumu](../backend/README.md)

## Kurulum

Node.js 22 veya üzeri gerekir. Komutları `web/` klasöründe çalıştır:

```powershell
npm ci
Copy-Item .env.example .env.local
npm run dev
```

- `API_URL`: Ortak backend'in sürümlenmiş adresi; varsayılan `http://127.0.0.1:5000/api/v1`. Sunucu tarafında kullanılır.
- `NEXT_PUBLIC_GOOGLE_CLIENT_ID`: İsteğe bağlı Google web OAuth istemcisi. Tanımlıysa Google giriş butonu görünür. Backend'in `GOOGLE_CLIENT_ID` değeriyle eşleşmeli ve web adresi Google Console'da yetkili JavaScript kaynağı olarak tanımlanmalıdır.
- Backend `FRONTEND_URL`: Şifre sıfırlama ve e-posta doğrulama bağlantılarının bu portala gelmesi için web adresi.

`http://localhost:3000` adresini aç. Backend'in hazır ve migration'larının uygulanmış olması gerekir. Web derlemesi API veya veritabanı bağlantısı gerektirmez.

## Üretim

```powershell
npm run build
npm start
```

Hosting çalışma dizini `web/` olmalıdır. HTTPS kullan. `API_URL` çalışan backend adresini göstermelidir. `NEXT_PUBLIC_*` değerleri build sırasında yerleştirildiğinden Google kimliği değiştiğinde yeniden derle.

## Sayfalar

Üretimde `WEB_PROXY_SECRET` backend ile aynı en az 32 karakterlik sunucu sırrı olmalıdır. `WEB_CLIENT_IP_HEADER`, güvenilen ingress'in ezerek sağladığı IP başlığıdır (Caddy kurulumunda `x-forwarded-for`). Next.js bu IP'yi zaman, yöntem ve yol ile HMAC imzalar; backend ham istemci başlıklarına güvenmez. Web sunucusunu ingress atlanarak internete açma. İstek sınırları API sunucuları arasında PostgreSQL'de paylaşılır; yeni migration'ları önce uygula.

| Yol                                                    | İçerik                                                                                  |
| ------------------------------------------------------ | --------------------------------------------------------------------------------------- |
| `/`                                                    | Genel bakış, günlük dersler, yaklaşan görevler ve hedef                                 |
| `/semesters`                                           | Dönem oluşturma, düzenleme ve silme                                                     |
| `/courses`                                             | Dersler, ağırlıklı not bileşenleri ve derse özel skala                                  |
| `/schedule`                                            | Haftalık ders programı                                                                  |
| `/attendance`                                          | Haftalık katılım/devamsızlık/mazeret kayıtları ve sınırlar                              |
| `/tasks`                                               | Görevler, durumlar ve alt görevler                                                      |
| `/calendar`                                            | Aylık takvim, sınavlar ve etkinlikler                                                   |
| `/gpa`                                                 | Kaydedilmiş notları değiştirmeyen GANO simülasyonu                                      |
| `/statistics`                                          | Dönem ortalamaları ve harf notu dağılımı                                                |
| `/transcript`                                          | Akademik özet, yazdır/PDF ve gözden geçirilerek PDF içe aktarma                         |
| `/settings`                                            | Tema, hedef GANO, sınav dönemi, skala, güvenlik, oturumlar, veri indirme ve hesap silme |
| `/help`                                                | Ortak API'den duyurular ve SSS                                                          |
| `/login`, `/register`                                  | Öğrenci giriş ve kayıt                                                                  |
| `/forgot-password`, `/reset-password`, `/verify-email` | Hesap kurtarma ve doğrulama                                                             |
| `/legal/*`                                             | Herkese açık yasal metinler ve web hesap silme yönergeleri                              |

## Güvenlik ve hata yönetimi

- Access ve refresh anahtarları `HttpOnly`, `SameSite=Lax` çerezlerde tutulur. Üretimde `Secure` kullanılır. Anahtarlar API yanıtından çıkarılır; localStorage'a yazılmaz.
- `/api/student/*` yalnızca açıkça izin verilen API yollarını ve yöntemlerini iletir. Keyfi URL'ye proxy yapılamaz.
- Veri değiştiren isteklerde aynı köken denetimi ve özel istek başlığı aranır.
- API ve sayfa verileri kullanıcılar arasında önbelleğe alınmaz. Gerçek yetkilendirme backend'de yapılır.
- Oturum yenileme sekmeler arasında Web Locks ile sıraya alınır. Backend, PostgreSQL üzerinde atomik yeniler; 10 saniyelik tekrar penceresinde farklı sunucular aynı aktif token'ı yeniden üretebilir. Sonraki tekrar kullanımlar oturumları iptal eder. Ham refresh token veritabanına yazılmaz.
- Ağ, doğrulama, boş liste, yüklenme ve tekrar deneme durumları arayüzde gösterilir.
- Gerçek zamanlı abonelik yoktur: veriler sayfa geçişinde, kayıt işlemi sonrasında ve sekmeye geri dönüldüğünde yenilenir. Web ve Android aynı kalıcı veriyi kullanır.
- Web bildirimleri ve çevrimdışı web desteği eklenmemiştir. Android hatırlatıcıları kendi istemcisinde çalışmaya devam eder.

## Doğrulama

```powershell
npm test
npm run lint
npm run typecheck
npm run build
npm run test:integration
npm run format:check
```

`tests/fixtures/api.mjs` yalnızca yerel test verisidir. Varsayılan uygulama bu API'yi kullanmaz. Entegrasyon testi PostgreSQL'e bağlanmadan çerezleri, giriş/çıkışı, CSRF korumasını, yetki aktarımını, kayıt işlemlerini ve anahtar yenilemeyi kontrol eder. Entegrasyon testinden önce `npm run build` çalıştırılmalıdır. Gerçek PostgreSQL ile kontrol için [backend kabul testi](../backend/README.md#kontroller) kullanılabilir; Google OAuth ve SMTP teslimatı ayrıca doğrulanmalıdır.

### Bağımlılık taraması

Üretim bağımlılıkları `npm audit --omit=dev` ile kontrol edilir:

```powershell
npm audit --omit=dev
npm audit
```

Geliştirme
bağımlılıklarının tam taramasında `eslint-config-next → fast-glob → braces`
zinciri için GHSA-vfj7-8cjw-p6xm uyarısı vardır. 15 Ekim 2026 itibarıyla
[yayımlanmış düzeltme yoktur](https://github.com/advisories/GHSA-vfj7-8cjw-p6xm).
Bu zincir lint aracında bulunur ve üretim bağımlılıklarına dahil değildir;
lint işlemini güvenilen depo dosyaları ve sabit glob desenleriyle çalıştır.
