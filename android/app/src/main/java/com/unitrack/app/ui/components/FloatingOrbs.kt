package com.unitrack.app.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

private data class Orb(
    val color: Color,
    val relativeRadius: Float,
    val startFraction: Offset,
    val endFraction: Offset,
    val periodMillis: Int
)

/**
 * Zeminde çok blur, çok büyük, 2-3 tane yavaşça süzülen "ışık topu" (madde #5
 * — Floating Background Objects). Her biri farklı sürede (30-46sn), farklı
 * bir köşeden diğerine yavaşça kayar; kullanıcı hareketi fark etmez ama arka
 * plan asla "ölü" durmaz.
 *
 * `Modifier.blur(...)`, Compose'ta yalnızca API 31+'da (RenderEffect)
 * gerçekten bulanıklaştırır; altındaki sürümlerde sessizce hiçbir şey
 * yapmaz (bkz. GlassCard.kt notu) — burada da aynı, bilinçli, ek bağımlılık
 * gerektirmeyen davranışa güveniyoruz. API 31 altında topların kenarı zaten
 * radial gradient ile yumuşak olduğu için görsel bütünlük bozulmuyor.
 */
@Composable
fun FloatingOrbs(modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "floating-orbs")

    val orbs = remember {
        listOf(
            Orb(
                color = Color(0xFF3D5CFF),
                relativeRadius = 0.55f,
                startFraction = Offset(0.05f, 0.10f),
                endFraction = Offset(0.35f, 0.30f),
                periodMillis = 34_000
            ),
            Orb(
                color = Color(0xFF8B5CFF),
                relativeRadius = 0.45f,
                startFraction = Offset(0.85f, 0.20f),
                endFraction = Offset(0.60f, 0.55f),
                periodMillis = 41_000
            ),
            Orb(
                color = Color(0xFF4ADE9E),
                relativeRadius = 0.35f,
                startFraction = Offset(0.30f, 0.85f),
                endFraction = Offset(0.65f, 0.65f),
                periodMillis = 46_000
            )
        )
    }

    val progresses = orbs.map { orb ->
        transition.animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = orb.periodMillis, easing = LinearEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "orb-progress"
        )
    }

    Canvas(
        modifier = modifier
            .fillMaxSize()
            .blur(80.dp)
    ) {
        orbs.forEachIndexed { index, orb ->
            val t = progresses[index].value
            val cx = size.width * (orb.startFraction.x + (orb.endFraction.x - orb.startFraction.x) * t)
            val cy = size.height * (orb.startFraction.y + (orb.endFraction.y - orb.startFraction.y) * t)
            val radius = size.minDimension * orb.relativeRadius

            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(orb.color.copy(alpha = 0.10f), Color.Transparent),
                    center = Offset(cx, cy),
                    radius = radius
                ),
                radius = radius,
                center = Offset(cx, cy)
            )
        }
    }
}
