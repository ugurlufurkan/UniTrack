package com.unitrack.app.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ProgressIndicatorDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp

/**
 * Bir ondalıklı sayıyı 0'dan hedef değere doğru animasyonlu biçimde sayarak
 * gösterir ("3.12" direkt belirmek yerine 0'dan yükselerek gelir). Ekranın
 * "canlı" hissetmesini sağlayan ucuz ama etkili bir detay.
 */
@Composable
fun AnimatedCounterText(
    targetValue: Float,
    modifier: Modifier = Modifier,
    style: TextStyle = MaterialTheme.typography.displayMedium,
    color: Color = Color.Unspecified,
    decimals: Int = 2,
    formatter: (Float) -> String = { "%.${decimals}f".format(it) }
) {
    // tween yerine spring — "0'dan sayarak gelen" sayı artık hedefi hafifçe
    // aşıp geri toparlanıyor, lineer bir sayaç gibi değil (madde #12).
    val animatedValue by animateFloatAsState(
        targetValue = targetValue,
        animationSpec = Motion.Gentle,
        label = "counter"
    )

    Text(text = formatter(animatedValue), style = style, color = color, modifier = modifier)
}

/**
 * Aynı sayaç mantığını tam sayılar için (ders/kredi/dönem sayıları gibi) kullanan
 * varyant — StatCard'larda "0" yerine yukarıdan sayarak gelen rakamlar için.
 */
@Composable
fun AnimatedCounterInt(
    targetValue: Int,
    modifier: Modifier = Modifier,
    style: TextStyle = MaterialTheme.typography.titleLarge
) {
    val animatedValue by animateFloatAsState(
        targetValue = targetValue.toFloat(),
        animationSpec = Motion.Snappy,
        label = "counter-int"
    )

    Text(text = animatedValue.toInt().toString(), style = style, modifier = modifier)
}

/**
 * GPA ilerleme çubuğu: mevcut GANO'nun, kullanıcının kendine koyduğu hedefe göre
 * ne kadarını kat ettiğini gösterir. 0'dan gerçek orana animasyonlu geçer.
 * targetGpa <= 0 durumunda (teorik olarak olmamalı, savunma amaçlı) çubuk boş kalır.
 */
@Composable
fun AnimatedGpaProgress(
    currentGpa: Double,
    targetGpa: Double,
    modifier: Modifier = Modifier,
    color: Color = ProgressIndicatorDefaults.linearColor
) {
    val fraction = if (targetGpa <= 0.0) {
        0f
    } else {
        (currentGpa / targetGpa).toFloat().coerceIn(0f, 1f)
    }

    // tween yerine spring: çubuk hedefe doğru lineer değil, hafif esneyerek
    // (biraz aşıp geri toparlanarak) ilerliyor — madde #12, Physics Animation.
    val animatedFraction by animateFloatAsState(
        targetValue = fraction,
        animationSpec = Motion.Gentle,
        label = "gpa-progress"
    )

    LinearProgressIndicator(
        progress = { animatedFraction },
        color = color,
        modifier = modifier.clip(RoundedCornerShape(8.dp)),
        strokeCap = StrokeCap.Round
    )
}
