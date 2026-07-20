package com.unitrack.app.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.graphics.rememberGraphicsLayer
import com.unitrack.app.ui.theme.rememberTimeOfDayGlowColors

/**
 * Uygulamanın tamamının arkasında duran, tek yerden yönetilen premium zemin:
 * düz koyu renk üzerine köşelerde YAVAŞÇA HAREKET EDEN mavi/mor "ambient"
 * ışıklar + 2-3 süzülen ışık topu (bkz. FloatingOrbs.kt) + çok hafif film
 * grain (bkz. NoiseTexture.kt).
 *
 * Işıkların konumu ~27-32 saniyede bir uçtan uca yavaşça kayıyor (madde #1 —
 * "Canlı Arka Plan"): kullanıcı hareketi doğrudan fark etmiyor ama zemin
 * asla statik/ölü durmuyor. Tüm animasyon `LinearEasing` + `Reverse` ile
 * sürekli, kesintisiz bir gidiş-geliş döngüsü kuruyor.
 *
 * GERÇEK GLASS (madde #6): Zemin, `rememberGraphicsLayer()` ile HER KAREDE
 * bir `GraphicsLayer`'a kaydediliyor (`layer.record { ... }`) ve normal
 * şekilde ekrana çizildikten sonra [LocalGlassBackdrop] üzerinden alt ağaca
 * (`content`) sağlanıyor. Ekranlardaki `GlassCard`'lar API 31+'da bu
 * katmanın kendi konumlarına denk gelen kısmını gerçekten bulanıklaştırıp
 * arkalarına çiziyor — yani artık gerçek bir "arkasını gören, bulanıklaştıran
 * cam" hissi var, sahte yarı saydamlık değil (bkz. GlassBackdrop.kt,
 * GlassCard.kt).
 */
@Composable
fun AmbientBackground(
    modifier: Modifier = Modifier,
    examWeek: Boolean = false,
    content: @Composable () -> Unit
) {
    val background = MaterialTheme.colorScheme.background
    val transition = rememberInfiniteTransition(label = "ambient-drift")
    val backdropLayer = rememberGraphicsLayer()

    // "Weather Effect" — cihaz saatine (ve varsa kullanıcının kendi girdiği
    // sınav haftasına) göre ışıkların sıcaklığı uyum sağlıyor (bkz.
    // TimeOfDayGlow.kt). Gerçek hava durumu verisi değil.
    val (dynamicGlowBlue, dynamicGlowViolet) = rememberTimeOfDayGlowColors(examWeek = examWeek)

    // Sol üst köşedeki mavi ışık, x ekseninde 0 → %18 arasında yavaşça kayar.
    val blueDriftX by transition.animateFloat(
        initialValue = 0f,
        targetValue = 0.18f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 32_000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "blue-drift-x"
    )

    // Sağ üstteki mor ışık farklı bir periyotta y ekseninde kayar — iki ışık
    // hiçbir zaman aynı anda aynı fazda olmadığı için hareket daha organik hissettiriyor.
    val violetDriftY by transition.animateFloat(
        initialValue = 0f,
        targetValue = 0.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 27_000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "violet-drift-y"
    )

    fun DrawScope.drawGlows() {
        val glowRadius = size.minDimension * 0.9f

        // Sol üst köşe: mavi ışık huzmesi (x ekseninde hafifçe kayan)
        drawRect(
            brush = Brush.radialGradient(
                colors = listOf(dynamicGlowBlue.copy(alpha = 0.16f), Color.Transparent),
                center = Offset(x = size.width * blueDriftX, y = 0f),
                radius = glowRadius
            )
        )

        // Sağ üst köşe: mor ışık huzmesi (y ekseninde hafifçe kayan)
        drawRect(
            brush = Brush.radialGradient(
                colors = listOf(dynamicGlowViolet.copy(alpha = 0.12f), Color.Transparent),
                center = Offset(x = size.width, y = size.height * violetDriftY),
                radius = glowRadius
            )
        )
    }

    Box(modifier = modifier.fillMaxSize()) {

        // --- Katman 1: yalnızca zemin (ışıklar + orb'lar + noise) ---
        // `drawWithContent` en dışta (zincirin başında) olduğu için, içindeki
        // `drawContent()` çağrısı bu Box'ın TÜM alt ağacını (background +
        // glows + noiseOverlay + FloatingOrbs children) `backdropLayer`'ın
        // offscreen tamponuna KAYDEDİYOR (record). Ardından `drawLayer(...)`
        // o kaydı gerçek ekrana çiziyor — görünürde hiçbir şey değişmiyor,
        // ama artık bu görüntü GlassCard'ların bulanıklaştırıp yeniden
        // kullanabileceği bir "anlık görüntü" olarak da mevcut (bkz.
        // GlassBackdrop.kt). Modifier SIRASI kritik: kayıt, background/
        // drawBehind/noiseOverlay'den ÖNCE (zincirin en başında) olmalı, yoksa
        // sadece children yakalanır, zemin yakalanmaz.
        Box(
            modifier = Modifier
                .fillMaxSize()
                .drawWithContent {
                    backdropLayer.record { this@drawWithContent.drawContent() }
                    drawLayer(backdropLayer)
                }
                .background(background)
                .drawBehind { drawGlows() }
                .noiseOverlay()
        ) {
            FloatingOrbs(modifier = Modifier.fillMaxSize())
        }

        // --- Katman 2: gerçek uygulama içeriği (ekranlar, GlassCard'lar) ---
        CompositionLocalProvider(LocalGlassBackdrop provides backdropLayer) {
            content()
        }
    }
}
