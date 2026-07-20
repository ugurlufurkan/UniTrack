package com.unitrack.app.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.unit.dp

/**
 * Madde #18 — Premium Skeleton: "Gray kutular değil, Apple Store gibi shimmer."
 *
 * Önceki sürüm tek-renk "nefes alan" (pulsating alpha) bir efektti — bunu
 * gerçek bir KAYDIRMALI shimmer'a çevirdik: soldan sağa doğru sürekli akan,
 * dar ve parlak bir ışık bandı (`Brush.linearGradient`), her karede
 * `translate` ile kaydırılıyor. Tüm skeleton kutuları (`base` rengi) AYNI
 * animasyon fazını paylaşıyor — bu yüzden band, bir kart listesinin
 * tamamının üzerinden tek, kesintisiz bir ışık huzmesi gibi geçiyor
 * (Apple Store/Facebook/LinkedIn'deki shimmer'ların çalışma mantığı budur).
 */
@Composable
private fun rememberShimmerBrush(): Brush {
    val transition = rememberInfiniteTransition(label = "skeleton-shimmer")
    val translateAnim by transition.animateFloat(
        initialValue = -400f,
        targetValue = 1200f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1100, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "skeleton-shimmer-translate"
    )

    val base = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)
    val highlight = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.22f)

    return Brush.linearGradient(
        colors = listOf(base, highlight, base),
        start = Offset(translateAnim, 0f),
        end = Offset(translateAnim + 300f, 300f)
    )
}

/**
 * Tek bir "kart" iskeleti: başlık + alt satır genişliğinde iki dikdörtgen.
 * Dashboard/Gpa/Transcript gibi kart-listesi ekranlarının ilk yüklemesinde
 * çıplak CircularProgressIndicator yerine kullanılır.
 */
@Composable
fun CardSkeletonItem(modifier: Modifier = Modifier) {
    val shimmerBrush = rememberShimmerBrush()

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(0.5f)
                .height(18.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(shimmerBrush)
        )
        Box(
            modifier = Modifier
                .fillMaxWidth(0.8f)
                .height(14.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(shimmerBrush)
        )
    }
}

/**
 * `itemCount` adet kart iskeletini alt alta dizer. Ekranın gerçek içerik
 * layoutuna (LazyColumn + 16dp padding + 10dp spacing) yakın durması için
 * varsayılan aralıklar aynı değerlerle eşleşiyor.
 */
@Composable
fun ListSkeleton(
    modifier: Modifier = Modifier,
    itemCount: Int = 5,
    contentPadding: PaddingValues = PaddingValues(16.dp)
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(contentPadding),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        repeat(itemCount) {
            CardSkeletonItem()
        }
    }
}
