package com.unitrack.app.ui.course

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.unitrack.app.ui.components.PressableIconButton
import com.unitrack.app.ui.components.PremiumTextButton
import com.unitrack.app.data.dto.GradeBandDto

/** Tek bir harf notu satırı için düzenlenebilir form state'i (örn. AA, min 90, katsayı 4.0). */
data class GradeBandFormState(
    var letter: String,
    var min: String,
    var point: String
)

fun List<GradeBandDto>.toFormState(): List<GradeBandFormState> =
    map { GradeBandFormState(it.letter, it.min.toString(), it.point.toString()) }

/**
 * Backend'deki gradeScaleSchema (Zod) ile birebir aynı kuralları burada da
 * uygular — önceden burada sadece "sayı mı, boş mu değil mi" bakılıyordu,
 * backend'in asıl reddettiği durumlar (aynı min iki kez, 0'dan başlayan
 * aralık yok, vb.) sunucuya gidene kadar hiç yakalanmıyordu.
 *
 * Geçersizse null döner ve [errorMessage] içine kullanıcının anlayacağı
 * bir açıklama yazar.
 */
fun List<GradeBandFormState>.toDtoOrNull(errorMessage: (String) -> Unit = {}): List<GradeBandDto>? {
    val bands = mapNotNull { state ->
        val min = state.min.toDoubleOrNull()
        val point = state.point.toDoubleOrNull()
        if (state.letter.isBlank() || min == null || point == null) null
        else GradeBandDto(letter = state.letter.trim(), min = min, point = point)
    }

    if (bands.size != size) {
        errorMessage("Tüm aralıklarda harf, min. puan ve katsayı dolu olmalı.")
        return null
    }
    if (bands.size < 2) {
        errorMessage("En az 2 harf notu aralığı girilmeli.")
        return null
    }
    if (bands.any { it.letter.length > 4 }) {
        errorMessage("Harf notu en fazla 4 karakter olabilir.")
        return null
    }
    if (bands.any { it.min < 0 || it.min > 100 }) {
        errorMessage("Min. puan 0-100 arasında olmalı.")
        return null
    }
    if (bands.any { it.point < 0 || it.point > 4.5 }) {
        errorMessage("Katsayı 0-4.5 arasında olmalı.")
        return null
    }
    if (bands.none { it.min == 0.0 }) {
        errorMessage("Skalada 0 puandan başlayan bir aralık (en düşük harf) olmalı.")
        return null
    }
    val mins = bands.map { it.min }
    if (mins.toSet().size != mins.size) {
        errorMessage("Aynı minimum puana sahip iki aralık olamaz — az önce eklediğin satırın min. puanını değiştirmeyi unutma.")
        return null
    }

    return bands
}

@Composable
fun GradeScaleEditor(
    bands: List<GradeBandFormState>,
    onAdd: () -> Unit,
    onRemove: (Int) -> Unit,
    onChange: (Int, GradeBandFormState) -> Unit
) {
    Column {
        Row(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = "Harf",
                style = MaterialTheme.typography.labelMedium,
                modifier = Modifier.weight(0.8f)
            )
            Text(
                text = "Min. Puan",
                style = MaterialTheme.typography.labelMedium,
                modifier = Modifier.weight(1f)
            )
            Text(
                text = "Katsayı",
                style = MaterialTheme.typography.labelMedium,
                modifier = Modifier.weight(1f)
            )
            Spacer(modifier = Modifier.height(1.dp))
        }

        Spacer(modifier = Modifier.height(6.dp))

        bands.forEachIndexed { index, band ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                OutlinedTextField(
                    value = band.letter,
                    onValueChange = { onChange(index, band.copy(letter = it)) },
                    modifier = Modifier.weight(0.8f),
                    singleLine = true
                )
                OutlinedTextField(
                    value = band.min,
                    onValueChange = { onChange(index, band.copy(min = it.filter { c -> c.isDigit() || c == '.' })) },
                    modifier = Modifier.weight(1f),
                    singleLine = true
                )
                OutlinedTextField(
                    value = band.point,
                    onValueChange = { onChange(index, band.copy(point = it.filter { c -> c.isDigit() || c == '.' })) },
                    modifier = Modifier.weight(1f),
                    singleLine = true
                )
                PressableIconButton(onClick = { onRemove(index) }) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = "Aralığı sil",
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }
            Spacer(modifier = Modifier.height(4.dp))
        }

        PremiumTextButton(onClick = onAdd) {
            Icon(Icons.Default.Add, contentDescription = null)
            Spacer(modifier = Modifier.height(1.dp))
            Text("Aralık ekle")
        }

        Text(
            text = "Not: en düşük harf notunun \"Min. Puan\" değeri 0 olmalı.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}