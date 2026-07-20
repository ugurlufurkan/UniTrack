package com.unitrack.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/**
 * Harf notunu, 4'lük not ortalaması puanına ([point]) göre anlamlı bir renge bağlar.
 * Course/Gpa/Transcript ekranlarının hepsi aynı "point" alanına sahip (CourseDto.gradePoint,
 * GpaCourseResult.point, TranscriptEntryDto.point) — o yüzden tek yerden yönetiliyor.
 *
 * Bilerek dinamik renk şemasının primary/tertiary'sini KULLANMIYORUZ: kullanıcının cihaz
 * duvar kağıdına göre "iyi" rengin bazen kırmızıya, "kötü" rengin yeşile denk gelmesi
 * kafa karıştırır. Trafik ışığı anlamı (yeşil/sarı/kırmızı) sabit kalmalı; sadece açık/koyu
 * tema için tonu ayarlıyoruz.
 */
@Composable
fun gradePointColor(point: Double?): Color {
    val dark = isSystemInDarkTheme()
    return when {
        point == null -> MaterialTheme.colorScheme.onSurfaceVariant
        point >= 3.0 -> if (dark) Color(0xFF81C784) else Color(0xFF2E7D32) // geçti, iyi
        point >= 2.0 -> if (dark) Color(0xFFFFD54F) else Color(0xFFB8860B) // sınırda
        else -> MaterialTheme.colorScheme.error // kaldı
    }
}

/**
 * Renk kodlu, yuvarlak köşeli harf notu rozeti: küçük bir renk noktası + harf.
 * Notlar henüz girilmemişse (letter/point null) nötr gri bir "—" gösterir; bunu
 * hata gibi kırmızıya boyamıyoruz çünkü henüz not girilmemiş olmak başarısızlık değil.
 */
@Composable
fun GradeChip(
    letter: String?,
    point: Double?,
    modifier: Modifier = Modifier
) {
    val color = gradePointColor(point)

    Row(
        modifier = modifier
            .clip(RoundedCornerShape(50))
            .background(color.copy(alpha = 0.14f))
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(color)
        )
        Text(
            text = letter ?: "—",
            style = MaterialTheme.typography.labelLarge,
            color = color
        )
    }
}
