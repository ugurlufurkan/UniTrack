package com.unitrack.app.ui.theme

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp

/**
 * "Adaptive Accent" (madde #26): tema tamamen değişmiyor, sadece GANO
 * performansına göre TEK bir vurgu rengi hafifçe yeşile ya da sarıya doğru
 * kayıyor — tıpkı "AA çoksa hafif yeşil ton, CC çoksa hafif sarı ton"
 * fikrindeki gibi. Marka rengi (AccentBlue) her zaman baskın kalıyor,
 * sadece ~%25-30 oranında harmanlanıyor; bu yüzden ekran bir anda "yeşil
 * uygulama"ya dönüşmüyor, sadece ince bir duygusal ipucu veriyor.
 *
 * Bantlar:
 *  - GANO >= 3.5  → AccentBlue, StatusSuccess'e doğru hafifçe kayar (başarı)
 *  - 2.5 <= GANO < 3.5 → saf AccentBlue (nötr)
 *  - GANO < 2.5   → AccentBlue, StatusWarning'e doğru hafifçe kayar (dikkat)
 *
 * Geçiş `animateColorAsState` ile yumuşatılıyor, böylece dönem güncellenip
 * GANO değiştiğinde renk de aniden değil akıcı biçimde kayıyor.
 */
@Composable
fun rememberAdaptiveAccentColor(gpa: Double, blendStrength: Float = 0.28f): State<Color> {
    val base = MaterialTheme.colorScheme.primary // AccentBlue (bkz. Theme.kt)

    val target = when {
        gpa >= 3.5 -> lerp(base, StatusSuccess, blendStrength)
        gpa < 2.5 -> lerp(base, StatusWarning, blendStrength)
        else -> base
    }

    return animateColorAsState(
        targetValue = target,
        animationSpec = tween(durationMillis = 600),
        label = "adaptive-accent"
    )
}
