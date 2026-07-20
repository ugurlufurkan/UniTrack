package com.unitrack.app.ui.components

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * Bottom nav ikonu: seçili değilken çizgi (outline) hali, seçiliyken dolu
 * (filled) hali gösterilir — Material You'nun "seçili sekme daha 'ağır'
 * görünür" prensibi. Geçiş anında ikon spring ile hafifçe büyüyüp normale
 * dönüyor (1.0 → 1.18 → 1.0), bir "dokunuş onayı" hissi veriyor.
 *
 * Not: iki farklı ImageVector arasında gerçek bir "morph" (şekil ara geçişi)
 * yapmıyoruz — Compose'da bu, path interpolation gerektirir ve iki farklı
 * ikonun kontrol noktası sayısı/sırası uyuşmadığından pratikte kırılgan/anlamsız
 * sonuçlar verir. Bunun yerine ani icon swap + spring scale kullanıyoruz;
 * gözle görülür şekil değişimi zaten swap'ın kendisiyle veriliyor, büyüme
 * animasyonu bunu yumuşatıyor.
 */
@Composable
fun AnimatedNavIcon(
    selected: Boolean,
    filled: ImageVector,
    outlined: ImageVector,
    contentDescription: String
) {
    val scale by animateFloatAsState(
        targetValue = if (selected) 1f else 0.92f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "nav-icon-scale"
    )

    Icon(
        imageVector = if (selected) filled else outlined,
        contentDescription = contentDescription,
        modifier = Modifier.graphicsLayer {
            scaleX = scale
            scaleY = scale
        }
    )
}
