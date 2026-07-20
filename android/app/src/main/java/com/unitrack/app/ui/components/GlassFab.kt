package com.unitrack.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Madde #21 — "Floating FAB (cam/glow)": düz Material3 `FloatingActionButton`
 * yerine, uygulamanın geri kalanıyla aynı "cam" diliyle (bkz. GlassCard.kt)
 * konuşan, etrafında hafif bir renkli parıltı (glow) olan bir FAB.
 *
 * Neden ayrı bir composable, `GlassCard(onClick=...)` değil: FAB dairesel ve
 * sabit boyutlu, `GlassCard` ise dikdörtgen/serbest boyutlu kartlar için.
 * İkisi de aynı temel dili (glassBlurBackdrop + pressScale + gradient
 * kenarlık) paylaşıyor ama farklı shape/boyut varsayımları var, bu yüzden
 * GlassCard'ı zorlamak yerine küçük, amaca özel bir bileşen daha temiz.
 */
@Composable
fun GlassFab(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = 56.dp,
    content: @Composable () -> Unit
) {
    val shape = CircleShape
    val interactionSource = remember { MutableInteractionSource() }
    val haptic = LocalHapticFeedback.current
    val primary = MaterialTheme.colorScheme.primary

    Box(
        modifier = modifier
            .size(size)
            // Glow: FAB'ın kendi rengini taşıyan, kendisinden biraz daha büyük,
            // çok soluk bir gölge katmanı — "ışık saçıyor" hissi verir.
            .shadow(
                elevation = 20.dp,
                shape = shape,
                clip = false,
                ambientColor = primary.copy(alpha = 0.55f),
                spotColor = primary.copy(alpha = 0.45f)
            )
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                role = Role.Button,
                onClick = {
                    haptic.click()
                    onClick()
                }
            )
            .pressScale(interactionSource, pressedScale = 0.92f)
            .clip(shape)
            .then(
                if (isRealBlurSupported) {
                    Modifier.glassBlurBackdrop(blurRadius = 20.dp)
                } else {
                    Modifier
                }
            )
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        primary.copy(alpha = 0.95f),
                        primary.copy(alpha = 0.85f)
                    )
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        // Çağıran taraf Icon()'a tint vermeyi unutsa bile, koyu/açık ne olursa
        // olsun FAB'ın primary zemini üzerinde okunur kalsın diye zorluyoruz.
        CompositionLocalProvider(LocalContentColor provides MaterialTheme.colorScheme.onPrimary) {
            content()
        }
    }
}
