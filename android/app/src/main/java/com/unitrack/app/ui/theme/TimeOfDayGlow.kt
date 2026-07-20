package com.unitrack.app.ui.theme

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import kotlinx.coroutines.delay
import java.time.LocalTime

/**
 * "Weather Effect" maddesinin karşılığı — ama gerçek hava durumu verisi
 * KULLANMIYORUZ. Backend'de hava durumu uç noktası yok ve buna yeni bir
 * konum izni + üçüncü parti API eklemek bu işin kapsamını haksız yere
 * büyütürdü. Onun yerine sadece CİHAZIN SAATİNE göre AmbientBackground'daki
 * köşe ışıklarının rengi/sıcaklığı günün saatine yumuşakça uyum sağlıyor:
 *
 *  - Sabah (05-08)  : ışıklar hafifçe altın/sıcak tona kayar
 *  - Gün ortası (09-16) : nötr marka renkleri (GlowBlue/GlowViolet)
 *  - Akşam (17-19)  : en sıcak an — gün batımı turuncusuna doğru belirgin kayma
 *  - Erken gece (20-22) : sıcaklık yavaşça söner
 *  - Derin gece (23-04) : ışıklar hem daha soluk hem daha koyu/doygun indigo
 *
 * Saat her 5 dakikada bir kontrol edilir (uygulama açık kalıp saat dilimini
 * geçtiğinde de güncellensin diye) ve renk geçişleri `animateColorAsState`
 * ile 3 saniyede yumuşakça harmanlanır — asla ani bir renk sıçraması olmaz.
 */
/**
 * "Weather Effect" maddesinin karşılığı — ama gerçek hava durumu verisi
 * KULLANMIYORUZ. Backend'de hava durumu uç noktası yok ve buna yeni bir
 * konum izni + üçüncü parti API eklemek bu işin kapsamını haksız yere
 * büyütürdü. Onun yerine sadece CİHAZIN SAATİNE göre AmbientBackground'daki
 * köşe ışıklarının rengi/sıcaklığı günün saatine yumuşakça uyum sağlıyor:
 *
 *  - Sabah (05-08)  : ışıklar hafifçe altın/sıcak tona kayar
 *  - Gün ortası (09-16) : nötr marka renkleri (GlowBlue/GlowViolet)
 *  - Akşam (17-19)  : en sıcak an — gün batımı turuncusuna doğru belirgin kayma
 *  - Erken gece (20-22) : sıcaklık yavaşça söner
 *  - Derin gece (23-04) : ışıklar hem daha soluk hem daha koyu/doygun indigo
 *
 * Saat her 5 dakikada bir kontrol edilir (uygulama açık kalıp saat dilimini
 * geçtiğinde de güncellensin diye) ve renk geçişleri `animateColorAsState`
 * ile 3 saniyede yumuşakça harmanlanır — asla ani bir renk sıçraması olmaz.
 *
 * [examWeek] true ise (kullanıcının kendi girdiği sınav haftası aralığı,
 * bkz. ExamPeriodPreferences/AmbientMoodViewModel) günün saatinden bağımsız
 * olarak ışıklara ek bir amber/kehribar tonu daha harmanlanır — "sınav
 * haftasındayım" hissi, günün hangi saati olursa olsun hafifçe hissediliyor,
 * ama zemin markanın mavi/mor kimliğini tamamen kaybetmiyor (sadece ~%35
 * oranında amber'e kayıyor, "turuncuya boyanmış" bir ekran değil).
 */
@Composable
fun rememberTimeOfDayGlowColors(examWeek: Boolean = false): Pair<Color, Color> {
    var hour by remember { mutableIntStateOf(LocalTime.now().hour) }

    LaunchedEffect(Unit) {
        while (true) {
            delay(5 * 60 * 1000L)
            hour = LocalTime.now().hour
        }
    }

    val warmth = when (hour) {
        in 5..8 -> 0.55f
        in 9..16 -> 0f
        in 17..19 -> 0.8f
        in 20..22 -> 0.25f
        else -> 0f
    }

    val isDeepNight = hour !in 6..21

    var targetBlue = lerp(GlowBlue, GlowSunset, warmth)
        .let { if (isDeepNight) lerp(it, GlowNight, 0.45f) else it }
    var targetViolet = lerp(GlowViolet, GlowSunset, warmth * 0.7f)

    if (examWeek) {
        targetBlue = lerp(targetBlue, StatusWarning, 0.35f)
        targetViolet = lerp(targetViolet, StatusWarning, 0.30f)
    }

    val animatedBlue by animateColorAsState(
        targetValue = targetBlue,
        animationSpec = tween(durationMillis = 3000),
        label = "glow-blue-time-of-day"
    )
    val animatedViolet by animateColorAsState(
        targetValue = targetViolet,
        animationSpec = tween(durationMillis = 3000),
        label = "glow-violet-time-of-day"
    )

    return animatedBlue to animatedViolet
}
