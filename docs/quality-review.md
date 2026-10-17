# Kod incelemesi ve doğrulama — 17 Ekim 2026

İnceleme; ortak API, web sunucu katmanı, öğrenci arayüzü, Android ağ/oturum/önbellek katmanları ve yayın araçlarını kapsar. Testler geçici PGlite/PostgreSQL veritabanları, MockWebServer ve izole tarayıcı hesaplarıyla çalıştırılmıştır. Canlı öğrenci veritabanına migration uygulanmamıştır.

## Düzeltilen sorunlar

| Alan                  | Sorun ve sonuç                                                                                                                                                                         |
| --------------------- | -------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| Hesap kurtarma        | Aynı sıfırlama/doğrulama bağlantısının eşzamanlı tüketilmesi engellendi. Anahtarlar özetlenerek saklanır.                                                                              |
| Şifre değişikliği     | Eski sıfırlama bağlantıları ve oturumlar atomik iptal edilir; eski şifreyi daha önce doğrulamış bir giriş isteği yeni oturum açamaz.                                                   |
| Tüm cihazlardan çıkış | Eşzamanlı token yenilemesinin yeni bir aktif oturum bırakması hesap kilidiyle engellendi. Gerçek PostgreSQL kilit yarışı test edildi.                                                  |
| Android oturumu       | Geç gelen yenileme yanıtları çıkış yapılmış veya başka hesapla değiştirilmiş oturumu yeniden yazamaz; eski istek yeni hesabın anahtarıyla tekrarlanmaz.                                |
| Android çıkışı        | Yerel çıkış ağ yanıtını beklemez; çevrimdışı çıkıştaki ağ hatası yakalanır.                                                                                                            |
| Android önbelleği     | Akademik kopyalar hesap kimliğiyle ayrılır; hesap değişirken dönen eski yanıt iptal edilir.                                                                                            |
| Android ayarları      | Tek tercih güncellemesi web veya başka cihazdaki diğer tercihleri eski yerel değerlerle ezmez.                                                                                         |
| Mobil istek gövdeleri | Kısmi ders, takvim, program, alt görev ve devamsızlık güncellemeleri dokunulmayan alanları `null` göndermez; bilerek temizlenen alanlar açıkça belirtilir.                             |
| Devamsızlık           | Aynı haftanın eşzamanlı kaydı çoğalmaz; yeni hafta/saat ayarları mevcut kayıtları geçersiz kılamaz.                                                                                    |
| Akademik kayıtlar     | Eşzamanlı dönem oluşturma/yeniden adlandırma ve ders düzenlemeleri sıralanır; not skalasıyla hesaplanan sonuçlar tutarlı kalır.                                                        |
| Sınav dönemi          | Tek tarih güncellendiğinde de kayıtlı diğer tarihle birlikte aralık doğrulanır.                                                                                                        |
| Planlama              | Eşzamanlı etkinlik, program ve alt görev güncellemeleri bağımsız alanları kaybetmez. Değişmeyen bildirimlerin kimlikleri korunur.                                                      |
| Takvim zamanı         | “Bugün” sunucunun yerel saatinden bağımsız `APP_TIMEZONE` ile hesaplanır; İstanbul gece yarısı ve yaz saati sınırları test edilir.                                                     |
| Sorgular              | Geçersiz/ters takvim tarihleri ve sınırsız/negatif sonuç limitleri reddedilir.                                                                                                         |
| Web yenilemesi        | `429`, geçici servis hataları ve bozuk başarı yanıtları geçerli çerezleri silmez.                                                                                                      |
| Web çıkışı            | Geçici API arızasında sunucu iptalinin tamamlanabilmesi için HttpOnly çerezler korunur; bekleyen çıkış özel istekleri engeller. Başarısız şifre değişikliği cihaz tercihlerini silmez. |
| Web hesap geçişi      | Gecikmiş eski istekler yeni hesabın çerezleriyle yeniden gönderilmez; oturum değişiminde diğer sekmelerin özel ekranları temizlenir.                                                   |
| Yükleme sınırı        | Web isteği tamamen belleğe alınmadan akış sırasında boyut sınırı uygulanır.                                                                                                            |
| E-posta               | Gönderim başarısızlığı “gönderildi” diye cevaplanmaz.                                                                                                                                  |
| Veri indirme          | Hesap dışa aktarmasına alt görevler eklenir; diğer hesapların verileri ve kimlik bilgileri alınmaz.                                                                                    |
| API belgesi           | Örnek bir canlı domaine istek göndermek yerine mevcut sunucunun sürümlenmiş adresi kullanılır.                                                                                         |

Güvenlik güncellemesinden önce oluşturulmuş e-posta doğrulama ve şifre sıfırlama bağlantıları için yeni bağlantı istenmelidir. Öğrenci hesapları ve akademik kayıtlar korunur.

Bozuk/aşırı büyük JSON gövdeleri beklenen 400/413 yanıtlarıyla reddedilir; parola içerebilen parser hataları loglanmaz. İzin verilmeyen CORS kaynakları 403 alır.

## Doğrulama

| Kontrol                                                                        | Sonuç                                                                                                                                 |
| ------------------------------------------------------------------------------ | ------------------------------------------------------------------------------------------------------------------------------------- |
| Backend TypeScript ve regresyonlar                                             | 33 test başarılı                                                                                                                      |
| Web birim/istemci testleri                                                     | 9 test başarılı                                                                                                                       |
| Web lint, TypeScript, biçim ve üretim derlemesi                                | Başarılı                                                                                                                              |
| Web proxy entegrasyonu                                                         | Başarılı                                                                                                                              |
| Chromium öğrenci, çevrimdışı, bildirim, hesap yalıtımı ve sekmeler arası çıkış | 5 senaryo başarılı                                                                                                                    |
| Android JVM testleri                                                           | 11 test başarılı                                                                                                                      |
| Android debug lint                                                             | Başarılı; mevcut araç/deprecation uyarıları sürüyor                                                                                   |
| Gerçek PostgreSQL                                                              | Migration, eşzamanlı içe aktarma/dönem/ders/devamsızlık/yenileme, tüm cihazlardan çıkış ve tek kullanımlık kurtarma kontrolü başarılı |
| Üretim Docker imajları                                                         | Hazır olma, imzalı gateway, kayıt, push worker ve hesap temizliği başarılı                                                            |
| Şifreli yedek                                                                  | age ile şifreleme, checksum ve geçici veritabanında geri yükleme başarılı                                                             |
| Sağlık monitorü                                                                | İki regresyon testi başarılı                                                                                                          |
| Üretim npm bağımlılıkları                                                      | Backend ve web için 0 bilinen açık                                                                                                    |

Komutlar kök, [backend](../backend/README.md#kontroller), [web](../web/README.md#doğrulama), [Android](../android/README.md#kontroller) ve [yayın](../ops/README.md) belgelerindedir. [GitHub Actions](../.github/workflows/ci.yml) aynı kontrolleri her push/PR'da tekrar çalıştırır.

## Açık kalan doğrulamalar

- Web geliştirme zincirindeki `braces` uyarısı 5 yüksek seviyeli npm bildirimi üretir. Üretim bağımlılıklarına dahil değildir. Ayrıntı ve mevcut yükseltme sınırı [web belgesinde](../web/README.md#bağımlılık-taraması) açıklanır.
- Domain/hosting henüz seçilmedi. Gerçek DNS/HTTPS yayını, Google hesabıyla giriş, SMTP teslimatı, tarayıcı sağlayıcısına gerçek push teslimatı ve imzalı Android release'in fiziksel cihaz kabulü ayrıca yapılmalıdır.
- Otomatik testler belirtilen senaryolar için kanıttır; tüm cihaz, ağ koşulu ve üniversite PDF biçimlerinin saha kabulünün yerine geçmez.
