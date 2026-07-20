# UniTrack Android

Öğrencinin kullandığı mobil uygulama.

**Teknolojiler:** Kotlin, Jetpack Compose, Hilt (dependency injection), Retrofit + OkHttp, Kotlin Coroutines/Flow, DataStore (yerel tercihler), WorkManager (hatırlatıcılar).

## Gereksinimler

- Android Studio (güncel bir sürüm)
- JDK 17+ (Android Studio genelde kendi getirir)

## Kurulum

1. Android Studio'da bu klasörü (`android/`) açın: **File → Open**
2. Gradle sync'in bitmesini bekleyin
3. `keystore.properties.example` dosyasını kopyalayıp `keystore.properties` yapın (debug build için genelde gerekmez, sadece release/imzalı build alırken lazım olur)

## Backend adresi

`app/build.gradle.kts` içinde `BASE_URL` build type'a göre değişir:

- **debug** → `http://10.0.2.2:5000/api/v1/` — Android emülatöründen bilgisayarınızın `localhost`'una işaret eden özel adres. Emülatörde çalıştırıyorsanız, backend'i bilgisayarınızda `npm run dev` ile ayağa kaldırmanız yeterli, başka bir ayar gerekmez.
- **release** → `https://api.unitrack.app/api/v1/` — gerçek prod domaini. Henüz bir sunucuya deploy edilmedi.

Gerçek bir telefondan (emülatör değil) test etmek isterseniz, `10.0.2.2` çalışmaz — bilgisayarınızın yerel ağ IP'sini (`ör. 192.168.1.x`) kullanmanız ve telefonla aynı Wi-Fi'de olmanız gerekir.

## Çalıştırma

1. Backend'in ayakta olduğundan emin olun (bkz. `../backend/README.md`)
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

## Bilinen özellikler

- Verilerimi dışa aktar: Dashboard'da tema seçicinin altında, backend'den tüm hesap verisini JSON olarak indirip paylaşım sayfasını açar.
- Tema/hedef GANO/sınav dönemi artık hesaba bağlı senkron — telefon değişince kaybolmaz.
