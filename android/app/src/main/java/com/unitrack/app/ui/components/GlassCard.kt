package com.unitrack.app.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.unitrack.app.ui.theme.Radius

/**
 * "Cam" görünümlü kart.
 *
 * GERÇEK GLASS (madde #6 — artık TAMAMLANDI): API 31+'da (Android 12+),
 * [LocalGlassBackdrop] üzerinden gelen, AmbientBackground'ın her karede
 * kaydettiği zemin görüntüsünü kendi ekran konumuna göre kaydırıp (translate)
 * gerçekten bulanıklaştırıp (`BlurEffect`, `RenderEffect` tabanlı) arkasına
 * çiziyor — yani kartın arkasında GERÇEKTEN ambient ışıklar/noise görünüyor
 * ve bulanık. API 26-30'da (RenderEffect yok) otomatik olarak eski yarı
 * saydam gradyan yöntemine dönüyor (`isRealBlurSupported` kontrolü) — dışarıdan
 * kullanan hiçbir ekran bunu bilmek zorunda değil.
 *
 * [onClick] verilirse kart dokunulabilir hale gelir VE basılınca spring ile
 * hafifçe küçülüp geri zıplar (madde #10/#12, bkz. Motion.kt). Varsayılan
 * `ripple` yerine bu tepkiyi kullanıyoruz çünkü cam yüzeyde standart daire
 * dalga efekti "premium" hissi bozuyor.
 *
 * PREMIUM SHADOW (madde #23 — TAMAMLANDI): tek bir gölge yerine 3 katman
 * (geniş/soluk "ambient" + orta + dar/belirgin "temas" gölgesi) üst üste
 * biniyor — gerçek dünyadaki gölgelerin birden fazla ışık kaynağından gelen
 * yumuşak birikimine daha yakın bir görünüm veriyor, tek katmanlı sert
 * gölgeye göre çok daha "havada duruyor" hissi.
 *
 * DOKUNMAYA TEPKİ VEREN KENARLIK (madde #22 — TAMAMLANDI): üstteki aydınlık
 * kenarlık, kart basılıyken hafifçe parlaklaşıyor — ışığın kartın üstüne o an
 * "vurduğu" hissini veren ince bir dokunsal ipucu.
 */
@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    cornerRadius: Dp = Radius.xl,
    onClick: (() -> Unit)? = null,
    content: @Composable () -> Unit
) {
    val shape = RoundedCornerShape(cornerRadius)
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val clickableModifier = if (onClick != null) {
        Modifier
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
            .pressScale(interactionSource)
    } else {
        Modifier
    }

    val backdrop = LocalGlassBackdrop.current

    val surfaceVariantColor = MaterialTheme.colorScheme.surfaceVariant
    val surfaceColor = MaterialTheme.colorScheme.surface

    val borderGlow by animateFloatAsState(
        targetValue = if (isPressed) 1f else 0f,
        animationSpec = Motion.Gentle,
        label = "border-glow"
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .then(clickableModifier)
            // Katman 1/3 — geniş, çok soluk "ambient occlusion" hissi
            .shadow(
                elevation = 32.dp,
                shape = shape,
                clip = false,
                ambientColor = Color.Black.copy(alpha = 0.35f),
                spotColor = Color.Transparent
            )
            // Katman 2/3 — orta mesafeli, biraz daha belirgin gölge
            .shadow(
                elevation = 16.dp,
                shape = shape,
                clip = false,
                ambientColor = Color.Black.copy(alpha = 0.30f),
                spotColor = Color.Black.copy(alpha = 0.25f)
            )
            // Katman 3/3 — kartın hemen altında, dar ve belirgin "temas" gölgesi
            .shadow(
                elevation = 6.dp,
                shape = shape,
                ambientColor = Color.Black.copy(alpha = 0.5f),
                spotColor = Color.Black.copy(alpha = 0.5f)
            )
            .clip(shape)
            .then(
                if (isRealBlurSupported && backdrop != null) {
                    Modifier
                        .glassBlurBackdrop()
                        .drawWithContent {
                            // Bulanık zeminin üstüne, camın kendi rengini/derinliğini
                            // veren çok hafif bir ton bindiriyoruz (tamamen şeffaf
                            // bulanıklık "cam" değil "delik" gibi görünürdü). Tint
                            // İÇERİKTEN ÖNCE çizilmeli, yoksa kartın metnini de örter.
                            drawRect(
                                brush = Brush.verticalGradient(
                                    colors = listOf(
                                        surfaceVariantColor.copy(alpha = 0.32f),
                                        surfaceColor.copy(alpha = 0.20f)
                                    )
                                )
                            )
                            drawContent()
                        }
                } else {
                    Modifier.background(
                        Brush.verticalGradient(
                            colors = listOf(
                                surfaceVariantColor.copy(alpha = 0.85f),
                                surfaceColor.copy(alpha = 0.65f)
                            )
                        )
                    )
                }
            )
            .border(
                width = 1.dp,
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color.White.copy(alpha = 0.22f + 0.16f * borderGlow),
                        Color.White.copy(alpha = 0.04f + 0.06f * borderGlow)
                    )
                ),
                shape = shape
            )
    ) {
        content()
    }
}
