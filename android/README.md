# UniTrack Android

Öğrencinin kullandığı mobil uygulama.

**Teknolojiler:** Kotlin, Jetpack Compose, Hilt (dependency injection), Retrofit + OkHttp, Kotlin Coroutines/Flow, DataStore (yerel tercihler), WorkManager (hatırlatıcılar).

## Gereksinimler

- Android Studio (güncel bir sürüm)
- JDK 21 (`app/build.gradle.kts` içindeki Java ve Kotlin toolchain ayarıyla aynı olmalı)
- Android SDK 36 (projenin `compileSdk` ve `targetSdk` değeri)

## Kurulum

1. Android Studio'da bu klasörü (`android/`) açın: **File → Open**
2. Gradle sync'in bitmesini bekleyin
3. `keystore.properties.example` dosyasını kopyalayıp `keystore.properties` yapın (debug build için genelde gerekmez, sadece release/imzalı build alırken lazım olur)

## Backend adresi

`app/build.gradle.kts` içinde `BASE_URL` build type'a göre değişir:

- **debug** → `http://10.0.2.2:5000/api/v1/` — Android emülatöründen bilgisayarınızın `localhost`'una işaret eden özel adres. Emülatörde çalıştırıyorsanız, backend'i bilgisayarınızda `npm run dev` ile ayağa kaldırmanız yeterli, başka bir ayar gerekmez.
- **release** → `UNITRACK_API_URL` ve `UNITRACK_WEB_URL` Gradle özelliklerinden alınır. Gerçek HTTPS adresleri ve `keystore.properties` verilmeden release derlemesi durur.

Gerçek bir telefondan (emülatör değil) test etmek isterseniz, `10.0.2.2` çalışmaz — bilgisayarınızın yerel ağ IP'sini (`ör. 192.168.1.x`) kullanmanız ve telefonla aynı Wi-Fi'de olmanız gerekir.

## Çalıştırma

1. Backend'in ayakta olduğundan emin olun (bkz. [backend kurulumu](../backend/README.md))
2. Android Studio'da bir emülatör (Device Manager) oluşturun/seçin
3. **Run** (yeşil ▶️ ikonu) — build type olarak **debug** seçili olmalı

## Proje yapısı

`app/src/main/java/com/unitrack/app/` altında:

- `data/api` — Retrofit servis arayüzleri
- `data/dto` — API'ye giden/gelen JSON modelleri
- `data/repository` — API + yerel depoyu birleştiren iş mantığı katmanı
- `data/local` — DataStore ile cihazda tutulan tercihler (tema, hedef GANO, sınav dönemi — artık backend'e de senkron ediliyor, bkz. `SettingsSyncRepository`)
- `di` — Hilt modülleri (network, work manager)
- `ui/*` — her özellik için ekran + ViewModel (dashboard, course, calendar, attendance, task, transcript, gpa, settings, ...)
- `notifications`, `widget`, `util` — bildirim/hatırlatıcı, ana ekran widget'ı, PDF/transkript export gibi yardımcılar

## Kontroller

```powershell
.\gradlew.bat :app:testDebugUnitTest :app:lintDebug
```

MockWebServer testleri GET tekrarlarını, yazma isteklerinin yinelenmemesini, geçici oturum servisi hatalarında hesabın korunmasını ve eşzamanlı isteklerde tek refresh yapılmasını doğrular. GitHub Actions JDK 21 ile aynı testleri ve lint kontrolünü çalıştırır. Gerçek Google girişinin ve imzalı release paketinin cihaz kabul testi ayrıca yapılmalıdır.

Ek regresyonlar; çıkıştan sonra gelen yenileme yanıtlarını, hesaplar arasında önbellek yalıtımını, kısmi ayar senkronunu ve mobil güncelleme gövdelerini kontrol eder. Gönderilmeyen alanlar korunur; isteğe bağlı alanların bilerek temizlenmesi ayrıca belirtilir. Ayrıntılar [kod inceleme raporundadır](../docs/quality-review.md).

## Bilinen özellikler

- Verilerimi dışa aktar: Dashboard'da tema seçicinin altında, backend'den tüm hesap verisini JSON olarak indirip paylaşım sayfasını açar.
- Tema/hedef GANO/sınav dönemi artık hesaba bağlı senkron — telefon değişince kaybolmaz.
