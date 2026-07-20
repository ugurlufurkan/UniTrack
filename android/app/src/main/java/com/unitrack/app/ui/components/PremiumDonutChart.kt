package com.unitrack.app.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** Tek bir dilim: değeri, rengi ve (erişilebilirlik/legend için) etiketi. */
data class DonutSegment(
    val value: Float,
    val color: Color,
    val label: String
)

/**
 * Madde #14 — Premium Charts: Apple Health'in "Activity Rings"ine yakın bir
 * dil — düz pasta/pie chart değil, YUVARLAK UÇLU, ARKASINDA HAFİF PARILTISI
 * (glow) olan bir halka grafiği. İki katman çiziyoruz:
 *  1) Bulanık (gerçek `Modifier.blur`, API 31+) bir "parıltı" kopyası —
 *     halkanın arkasından taşan ışık hissi
 *  2) Üstte keskin, gerçek halka
 *
 * Değerler, ekrana girerken 0'dan gerçek oranlarına doğru [Motion.Gentle]
 * (spring) ile "çizilerek" beliriyor — statik bir pasta grafiği değil.
 *
 * [segments] boşsa veya toplam 0 ise nötr bir gri halka çizilir (bölme
 * hatası yerine sessizce zarif bir boş durum).
 */
@Composable
fun PremiumDonutChart(
    segments: List<DonutSegment>,
    modifier: Modifier = Modifier,
    size: Dp = 140.dp,
    strokeWidth: Dp = 16.dp,
    centerContent: @Composable () -> Unit = {}
) {
    val total = segments.sumOf { it.value.toDouble() }.toFloat()
    val progress = remember { Animatable(0f) }

    LaunchedEffect(segments) {
        progress.snapTo(0f)
        progress.animateTo(1f, animationSpec = Motion.Gentle)
    }

    Box(
        modifier = modifier.size(size),
        contentAlignment = Alignment.Center
    ) {
        // Katman 1: bulanık parıltı kopyası.
        Canvas(
            modifier = Modifier
                .size(size)
                .blur(18.dp)
        ) {
            drawDonutRing(
                segments = segments,
                total = total,
                progress = progress.value,
                strokeWidthPx = strokeWidth.toPx() * 1.4f,
                alphaMultiplier = 0.55f
            )
        }

        // Katman 2: keskin, gerçek halka.
        Canvas(modifier = Modifier.size(size)) {
            drawDonutRing(
                segments = segments,
                total = total,
                progress = progress.value,
                strokeWidthPx = strokeWidth.toPx(),
                alphaMultiplier = 1f
            )
        }

        centerContent()
    }
}

private fun DrawScope.drawDonutRing(
    segments: List<DonutSegment>,
    total: Float,
    progress: Float,
    strokeWidthPx: Float,
    alphaMultiplier: Float
) {
    if (segments.isEmpty() || total <= 0f) {
        drawArc(
            color = Color.Gray.copy(alpha = 0.15f * alphaMultiplier),
            startAngle = 0f,
            sweepAngle = 360f,
            useCenter = false,
            style = Stroke(width = strokeWidthPx, cap = StrokeCap.Round)
        )
        return
    }

    var startAngle = -90f
    for (segment in segments) {
        val fullSweep = (segment.value / total) * 360f
        drawArc(
            color = segment.color.copy(alpha = segment.color.alpha * alphaMultiplier),
            startAngle = startAngle,
            sweepAngle = fullSweep * progress,
            useCenter = false,
            style = Stroke(width = strokeWidthPx, cap = StrokeCap.Round)
        )
        startAngle += fullSweep
    }
}
