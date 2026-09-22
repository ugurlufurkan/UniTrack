package com.unitrack.app.ui.splash

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.unitrack.app.R
import kotlinx.coroutines.delay
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

// ── Renk paleti ────────────────────────────────────────────
private val BgDark = Color(0xFF000515)
private val BgMid = Color(0xFF030B2E)
private val BgDeep = Color(0xFF0A1647)
private val AccentBlue = Color(0xFF4A9EFF)
private val AccentRed = Color(0xFFEF4444)
private val AccentGreen = Color(0xFF22C55E)
private val AccentAmber = Color(0xFFF59E0B)
private val AccentCyan = Color(0xFF06B6D4)
private val TextMuted = Color(0xFF64748B)
private val TextLight = Color(0xFF94A3B8)

/**
 * Sinematik açılış: 5 saniye. 4 sahne + dönen yuvarlak logo kapanışı.
 */
@Composable
fun AnimatedSplashScreen(
    onSplashFinished: () -> Unit
) {
    // ── Zamanlama (5000ms toplam) ───────────────────────────
    // 0..1000     Sahne 1: Devamsızlık uyarısı
    // 1000..2000  Sahne 2: GPA hedefi
    // 2000..3000  Sahne 3: Transkript
    // 3000..4000  Sahne 4: Final notu
    // 3800..5000  Logo (yuvarlak, dönen) + fade-out
    var elapsed by remember { mutableLongStateOf(0L) }

    LaunchedEffect(Unit) {
        val startTime = System.currentTimeMillis()
        while (elapsed < 5000L) {
            delay(16L)
            elapsed = System.currentTimeMillis() - startTime
        }
        onSplashFinished()
    }

    val phase = elapsed

    // ── Arka plan ──────────────────────────────────────────
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(BgDark, BgMid, BgDeep, BgDark)
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        // ── Yüzen parçacıklar ─────────────────────────────
        BackgroundParticles(alpha = if (phase < 3800) 0.35f else 0.6f, phase = phase)

        // ── SAHNE 1: GPA ─────────────────────────────────
        SceneWithTiming(phase, start = 0, end = 1600, fadeIn = 400, fadeOut = 400) { progress, alpha ->
            SceneGpaTarget(progress = progress, alpha = alpha)
        }

        // ── SAHNE 2: Transkript ───────────────────────────
        SceneWithTiming(phase, start = 1200, end = 2800, fadeIn = 400, fadeOut = 400) { progress, alpha ->
            SceneTranscript(progress = progress, alpha = alpha)
        }

        // ── SAHNE 3: Final ───────────────────────────────
        SceneWithTiming(phase, start = 2400, end = 4000, fadeIn = 400, fadeOut = 400) { progress, alpha ->
            SceneFinalRequired(progress = progress, alpha = alpha)
        }

        // ── LOGO REVEAL (yuvarlak, dönen) ────────────────
        if (phase > 3600) {
            val logoProgress = ((phase - 3600) / 1000f).coerceIn(0f, 1f)
            val logoFadeOut = if (phase > 4600) 1f - ((phase - 4600) / 400f).coerceIn(0f, 1f) else 1f
            LogoReveal(progress = logoProgress, alpha = logoFadeOut)
        }
    }
}

// ════════════════════════════════════════════════════════════
//  Sahne zamanlama yardımcısı
// ════════════════════════════════════════════════════════════
@Composable
private fun BoxScope.SceneWithTiming(
    phase: Long,
    start: Long,
    end: Long,
    fadeIn: Long,
    fadeOut: Long,
    content: @Composable BoxScope.(progress: Float, alpha: Float) -> Unit
) {
    if (phase < start - fadeIn || phase > end) return
    val duration = end - start - fadeOut
    val sceneProgress = ((phase - start).coerceIn(0, duration) / duration.toFloat())
    val alphaIn = ((phase - (start - fadeIn)).coerceIn(0, fadeIn) / fadeIn.toFloat())
    val alphaOut = if (phase > end - fadeOut) {
        1f - ((phase - (end - fadeOut)).coerceIn(0, fadeOut) / fadeOut.toFloat())
    } else 1f
    content(FastOutSlowInEasing.transform(sceneProgress), alphaIn * alphaOut)
}

// ════════════════════════════════════════════════════════════
//  SAHNE 2 — GPA Hedefi
// ════════════════════════════════════════════════════════════
@Composable
private fun BoxScope.SceneGpaTarget(progress: Float, alpha: Float) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.alpha(alpha)
    ) {
        // Grafik
        Canvas(
            modifier = Modifier
                .size(width = 180.dp, height = 90.dp)
                .alpha(progress)
        ) {
            val points = listOf(0.3f, 0.25f, 0.4f, 0.35f, 0.55f, 0.65f, 0.6f, 0.75f, 0.85f)
            val visibleCount = (points.size * progress).toInt().coerceAtLeast(1)
            val path = Path()
            val stepX = size.width / (points.size - 1)

            points.take(visibleCount).forEachIndexed { i, value ->
                val x = i * stepX
                val y = size.height * (1f - value)
                if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
            }

            drawPath(
                path = path,
                color = AccentGreen,
                style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
            )

            // Son nokta glow
            if (visibleCount > 0) {
                val lastIdx = visibleCount - 1
                val lx = lastIdx * stepX
                val ly = size.height * (1f - points[lastIdx])
                drawCircle(AccentGreen.copy(alpha = 0.25f), radius = 14.dp.toPx(), center = Offset(lx, ly))
                drawCircle(AccentGreen, radius = 4.dp.toPx(), center = Offset(lx, ly))
            }
        }

        Spacer(Modifier.height(18.dp))

        Row(verticalAlignment = Alignment.Bottom) {
            Text(
                text = "Hedef GPA: ",
                color = TextLight.copy(alpha = progress),
                fontSize = 17.sp
            )
            Text(
                text = "3.50",
                color = AccentGreen.copy(alpha = progress),
                fontSize = 30.sp,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(Modifier.height(8.dp))

        Text(
            text = "Yolundasın.",
            color = TextMuted.copy(alpha = progress * 0.9f),
            fontSize = 14.sp
        )
    }
}

// ════════════════════════════════════════════════════════════
//  SAHNE 3 — Transkript → Veri
// ════════════════════════════════════════════════════════════
@Composable
private fun BoxScope.SceneTranscript(progress: Float, alpha: Float) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.alpha(alpha)
    ) {
        Canvas(
            modifier = Modifier
                .size(width = 200.dp, height = 100.dp)
                .alpha(progress)
        ) {
            val lines = 6
            for (i in 0 until lines) {
                val yPos = (i.toFloat() / lines) * size.height + 10.dp.toPx()
                val lineAlpha = (1f - progress * 0.7f).coerceIn(0.08f, 0.5f)

                // Sol: PDF satırı (soluklaşıyor)
                val leftWidth = size.width * 0.35f * (1f - progress * 0.6f)
                drawLine(
                    color = TextMuted.copy(alpha = lineAlpha),
                    start = Offset(0f, yPos),
                    end = Offset(leftWidth, yPos),
                    strokeWidth = 2.5f.dp.toPx(),
                    cap = StrokeCap.Round
                )

                // Sağ: veri noktaları (belirginleşiyor)
                val dotX = size.width * 0.45f + (i * 20.dp.toPx()) * progress
                val dotAlpha = (progress - 0.2f).coerceIn(0f, 1f)
                val dotColor = when (i % 3) {
                    0 -> AccentBlue
                    1 -> AccentGreen
                    else -> AccentCyan
                }
                // Glow
                drawCircle(
                    color = dotColor.copy(alpha = dotAlpha * 0.2f),
                    radius = (6f + progress * 3f).dp.toPx(),
                    center = Offset(dotX, yPos)
                )
                drawCircle(
                    color = dotColor.copy(alpha = dotAlpha * 0.85f),
                    radius = (3f + progress * 1.5f).dp.toPx(),
                    center = Offset(dotX, yPos)
                )
            }
        }

        Spacer(Modifier.height(18.dp))

        Text(
            text = "Transkript yüklendi",
            color = AccentCyan.copy(alpha = progress),
            fontSize = 18.sp,
            fontWeight = FontWeight.SemiBold
        )

        Spacer(Modifier.height(8.dp))

        Text(
            text = "PDF → veriye dönüştürüldü.",
            color = TextMuted.copy(alpha = progress * 0.9f),
            fontSize = 14.sp
        )
    }
}

// ════════════════════════════════════════════════════════════
//  SAHNE 4 — Final Notu
// ════════════════════════════════════════════════════════════
@Composable
private fun BoxScope.SceneFinalRequired(progress: Float, alpha: Float) {
    val displayNumber = (72 * progress).toInt()

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .alpha(alpha)
            .offset(y = ((1f - progress) * 20).dp)
    ) {
        Text(
            text = "Finalde Gereken",
            color = TextLight.copy(alpha = progress),
            fontSize = 15.sp
        )

        Spacer(Modifier.height(10.dp))

        Text(
            text = "$displayNumber",
            color = AccentAmber.copy(alpha = progress),
            fontSize = 60.sp,
            fontWeight = FontWeight.Bold
        )

        Spacer(Modifier.height(10.dp))

        // Progress bar
        Box(
            modifier = Modifier
                .width(140.dp)
                .height(4.dp)
                .clip(RoundedCornerShape(999.dp))
                .background(TextMuted.copy(alpha = 0.2f))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(progress * 0.72f)
                    .clip(RoundedCornerShape(999.dp))
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(AccentAmber, AccentAmber.copy(alpha = 0.5f))
                        )
                    )
            )
        }

        Spacer(Modifier.height(14.dp))

        Text(
            text = "Geçmen için yeterli.",
            color = TextMuted.copy(alpha = progress * 0.9f),
            fontSize = 14.sp
        )
    }
}

// ════════════════════════════════════════════════════════════
//  LOGO REVEAL — Yuvarlak, dönen, glow efektli
// ════════════════════════════════════════════════════════════
@Composable
private fun BoxScope.LogoReveal(progress: Float, alpha: Float) {
    val eased = FastOutSlowInEasing.transform(progress)

    // Dönen halka (Logonun kendisi de dönebilir, etrafındaki halka da)
    val infiniteTransition = rememberInfiniteTransition(label = "logoSpin")
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(2500, easing = LinearEasing)
        ),
        label = "rotation"
    )

    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.15f,
        targetValue = 0.6f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glowAlpha"
    )

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier.alpha(alpha)
    ) {
        // Logo ve etrafındaki dönen halka tam olarak birbirine hizalı (Aynı Box içindeler)
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.size(180.dp)
        ) {
            // Glow arka plan
            Canvas(modifier = Modifier.fillMaxSize().alpha(eased)) {
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            AccentBlue.copy(alpha = glowAlpha),
                            AccentBlue.copy(alpha = glowAlpha * 0.2f),
                            Color.Transparent
                        )
                    )
                )
            }

            // Etraftaki dönen ince halkalar
            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .rotate(rotation)
                    .alpha(eased * 0.8f)
            ) {
                drawArc(
                    color = AccentBlue.copy(alpha = 0.8f),
                    startAngle = 0f,
                    sweepAngle = 120f,
                    useCenter = false,
                    style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
                )
                drawArc(
                    color = AccentCyan.copy(alpha = 0.6f),
                    startAngle = 180f,
                    sweepAngle = 80f,
                    useCenter = false,
                    style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round)
                )
            }

            // Logonun Kendisi
            Image(
                painter = painterResource(id = R.mipmap.ic_launcher_foreground),
                contentDescription = "UniTrack Logo",
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(140.dp)
                    // Ekstra havalı: Logo hafifçe kendi etrafında yavaşça dönsün, büyüsün
                    .scale(0.4f + eased * 0.6f + (if (progress > 0.8f) (progress - 0.8f) * 2f else 0f))
                    .rotate(rotation * -0.2f) // Halkanın tersine çok hafif bir dönüş
                    .alpha(eased)
                    .clip(CircleShape)
            )
        }

        Spacer(Modifier.height(24.dp))

        Text(
            text = "UniTrack",
            fontSize = 36.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White.copy(alpha = eased),
            modifier = Modifier.offset(y = ((1f - eased) * 15).dp)
        )

        Spacer(Modifier.height(8.dp))

        Text(
            text = "Akademik hayatını kontrol altına al.",
            fontSize = 14.sp,
            color = TextLight.copy(alpha = eased * 0.9f),
            modifier = Modifier.offset(y = ((1f - eased) * 10).dp)
        )
    }
}

// ════════════════════════════════════════════════════════════
//  ARKA PLAN PARÇACİKLARI
// ════════════════════════════════════════════════════════════
@Composable
private fun BackgroundParticles(alpha: Float, phase: Long) {
    data class Particle(
        val x: Float, val y: Float, val radius: Float,
        val speed: Float, val angle: Float, val baseAlpha: Float
    )

    val particles = remember {
        List(30) {
            Particle(
                x = Random.nextFloat(),
                y = Random.nextFloat(),
                radius = Random.nextFloat() * 2.5f + 0.5f,
                speed = Random.nextFloat() * 0.12f + 0.03f,
                angle = Random.nextFloat() * 2f * PI.toFloat(),
                baseAlpha = Random.nextFloat() * 0.35f + 0.08f
            )
        }
    }

    val time = phase / 1000f

    Canvas(modifier = Modifier.fillMaxSize().alpha(alpha)) {
        particles.forEach { p ->
            val px = ((p.x + cos(p.angle + time * p.speed) * 0.1f) % 1f) * size.width
            val py = ((p.y + sin(p.angle + time * p.speed) * 0.1f) % 1f) * size.height
            // Glow
            drawCircle(
                color = AccentBlue.copy(alpha = p.baseAlpha * 0.3f),
                radius = (p.radius * 3f).dp.toPx(),
                center = Offset(px.coerceIn(0f, size.width), py.coerceIn(0f, size.height))
            )
            drawCircle(
                color = AccentBlue.copy(alpha = p.baseAlpha),
                radius = p.radius.dp.toPx(),
                center = Offset(px.coerceIn(0f, size.width), py.coerceIn(0f, size.height))
            )
        }
    }
}
