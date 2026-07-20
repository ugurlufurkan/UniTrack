package com.unitrack.app.ui.theme

import androidx.compose.ui.graphics.Color

// ---------------------------------------------------------------------------
// UniTrack Premium Palette
// İlham: Apple'ın sadeliği (neredeyse siyah zemin, beyaza yakın metin),
// Tesla'nın minimalizmi (tek bir vurgu rengi, gereksiz renk yok),
// Nothing OS'in modern detayı (yüksek kontrast, ince çizgiler, mat yüzeyler).
//
// dynamicColor KAPALI (bkz. Theme.kt) — telefonun duvar kağıdına göre renk
// değişmesi premium/marka hissini bozar, Apple/Tesla/Nothing uygulamaları
// hiçbiri bunu yapmaz. Palet sabit ve bilinçli seçilmiş.
// ---------------------------------------------------------------------------

// Zemin: saf siyah değil, çok hafif mavimsi neredeyse-siyah (OLED dostu,
// Apple'ın "kağıt beyazı" yerine "space black" mantığının koyu tema karşılığı)
val SpaceBlack = Color(0xFF08080D)
val SurfaceDark = Color(0xFF121218)
val SurfaceElevatedDark = Color(0xFF1A1A22)

// Ambient köşe ışıkları için (AmbientBackground.kt içinde kullanılır)
val GlowBlue = Color(0xFF3D5CFF)
val GlowViolet = Color(0xFF8B5CFF)

// Zamana duyarlı ambient ton kayması için (bkz. TimeOfDayGlow.kt): gerçek
// hava durumu verisi YOK (backend'de böyle bir uç nokta yok, cihaza yeni bir
// izin/API bağımlılığı eklemek istemedik) — bunun yerine sadece cihaz saatine
// göre ışıkların "sıcaklığı" günün saatine uyum sağlıyor.
val GlowSunset = Color(0xFFFF8A5C)  // gündoğumu/günbatımı — sıcak turuncu-somon
val GlowNight = Color(0xFF2A2ECC)   // derin gece — daha koyu, doygun indigo

// Ana vurgu: elektrik mavisi-indigo (Tesla'nın "highland" mavisine yakın,
// ama dijital/parlak) — butonlar, aktif sekme, ilerleme çubukları
val AccentBlue = Color(0xFF6C8CFF)
val AccentBlueDim = Color(0xFF4A5FCC)

// İkincil vurgu: mor — grafiklerde/rozetlerde çeşitlilik için
val AccentViolet = Color(0xFFB18CFF)

// Durum renkleri (not/GPA bağlamında anlamlı): başarı=turkuaz-yeşil,
// uyarı=amber, hata=mercan kırmızı — hepsi koyu zeminde parlayacak şekilde
// doygunluğu düşürülmüş, göz yormayan tonlar
val StatusSuccess = Color(0xFF4ADE9E)
val StatusWarning = Color(0xFFFFC168)
val StatusError = Color(0xFFFF7A7A)

// Metin: Apple'ın "label" hiyerarşisine benzer üç seviye
val TextPrimary = Color(0xFFF5F5F7)
val TextSecondary = Color(0xFFA0A0AD)
val TextTertiary = Color(0xFF6C6C78)

// Açık tema (sistem açık temadaysa / dynamicColor kapalıyken fallback) —
// koyu tema birincil hedef olsa da açık tema tamamen çıplak bırakılmıyor
val LightBackground = Color(0xFFF7F7FA)
val LightSurface = Color(0xFFFFFFFF)
val LightAccentBlue = Color(0xFF4A5FE0)
