package com.unitrack.app.ui.components

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer

/**
 * Uygulama genelinde tekrar kullanılan "physics" (spring) animasyon ayarları.
 *
 * Şimdiye kadar projedeki tüm animasyonlar `tween(...)` — yani sabit süreli,
 * lineer/ease eğrili — kullanıyordu. Bu, "premium" hissin en büyük eksiği:
 * Apple/Arc/Linear gibi uygulamalardaki o hafif "zıplama", esneme hissi
 * gerçek fizik tabanlı (spring) animasyonlardan geliyor, sabit süreli
 * easing'lerden değil. Buradaki spec'ler tek yerden yönetiliyor ki tüm
 * ekranlardaki dokunuş/geçiş animasyonları aynı "malzeme" hissini paylaşsın.
 */
object Motion {

    /** Kart/buton basılma tepkisi gibi çok hızlı, belirgin geri tepmeli etkiler için. */
    val Snappy = spring<Float>(
        dampingRatio = Spring.DampingRatioMediumBouncy,
        stiffness = Spring.StiffnessHigh
    )

    /** Sayfa/kart girişleri, büyüyerek beliren diyaloglar gibi daha yumuşak etkiler için. */
    val Gentle = spring<Float>(
        dampingRatio = Spring.DampingRatioLowBouncy,
        stiffness = Spring.StiffnessMediumLow
    )

    /** Renk/ilerleme çubuğu gibi görsel değerlerin akıcı ama gecikmesiz geçişi için. */
    val Smooth = spring<Float>(
        dampingRatio = Spring.DampingRatioNoBouncy,
        stiffness = Spring.StiffnessMedium
    )
}

/**
 * Bir öğeye dokunulduğunda hafifçe küçülüp (1.00 → 0.97), bırakıldığında
 * spring ile geri "zıplayarak" 1.00'e dönmesini sağlar (madde #10 — Gesture
 * Animation, madde #12 — Physics Animation). `interactionSource` dışarıdan
 * verilirse aynı kaynak `clickable`/`Modifier.combinedClickable` ile
 * paylaşılabilir; verilmezse sadece görsel basılma durumu için kendi
 * kaynağını oluşturur (tıklamayı YÖNETMEZ — tıklama davranışı çağıran
 * tarafın `clickable` modifier'ında kalır).
 */
@Composable
fun Modifier.pressScale(
    interactionSource: MutableInteractionSource? = null,
    pressedScale: Float = 0.97f
): Modifier {
    val source = interactionSource ?: remember { MutableInteractionSource() }
    val isPressed by source.collectIsPressedAsState()

    val scale by animateFloatAsState(
        targetValue = if (isPressed) pressedScale else 1f,
        animationSpec = Motion.Snappy,
        label = "press-scale"
    )

    return this.graphicsLayer {
        scaleX = scale
        scaleY = scale
    }
}
