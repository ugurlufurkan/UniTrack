package com.unitrack.app.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.LocalContentColor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp

/**
 * `IconButton`'ın yerine geçen, standart daire ripple yerine spring tabanlı
 * basılma (`pressScale`) tepkisi veren varyant (madde #10 — Gesture
 * Animation, madde #16 — Micro Interactions: "Her buton ripple değil, kendi
 * animasyonu"). Düzenle/sil gibi sık kullanılan aksiyon ikonlarında kullanılıyor.
 */
@Composable
fun PressableIconButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    tint: Color = LocalContentColor.current,
    size: androidx.compose.ui.unit.Dp = 40.dp,
    content: @Composable () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }

    Box(
        modifier = modifier
            .size(size)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                role = Role.Button,
                onClick = onClick
            )
            .pressScale(interactionSource, pressedScale = 0.88f),
        contentAlignment = Alignment.Center
    ) {
        CompositionLocalProvider(LocalContentColor provides tint) {
            content()
        }
    }
}
