package com.unitrack.app.ui.components

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.RowScope
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier

/**
 * Madde #16 — Micro Interactions: "Her buton ripple değil, kendi animasyonu."
 *
 * `PressableIconButton` yalnızca ikon butonlarını kapsıyordu; diyaloglardaki
 * "Kaydet"/"Vazgeç"/"Ekle" gibi asıl metin butonları hâlâ düz Material
 * ripple kullanıyordu. Bu iki sarmalayıcı, Material3'ün kendi ripple'ını
 * KALDIRMIYOR (Button/TextButton'da bunu kapatan bir parametre yok) — onun
 * ÜSTÜNE spring tabanlı hafif bir basılma küçülmesi (`pressScale`) ekliyor.
 * İkisi birlikte, tek başına ripple'dan daha "premium" hissettiriyor; Compose
 * Button'ın kendi davranışını (erişilebilirlik, durum yönetimi, renkler) hiç
 * bozmadan.
 *
 * Kullanım, orijinal `Button`/`TextButton` ile birebir aynı — sadece isim
 * değişiyor, bu yüzden mevcut çağrı yerlerinde parametre uyuşmazlığı riski yok.
 */
@Composable
fun PressableButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    contentPadding: PaddingValues = ButtonDefaults.ContentPadding,
    content: @Composable RowScope.() -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    Button(
        onClick = onClick,
        modifier = modifier.pressScale(interactionSource),
        enabled = enabled,
        contentPadding = contentPadding,
        interactionSource = interactionSource,
        content = content
    )
}

@Composable
fun PressableTextButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    content: @Composable RowScope.() -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    TextButton(
        onClick = onClick,
        modifier = modifier.pressScale(interactionSource),
        enabled = enabled,
        interactionSource = interactionSource,
        content = content
    )
}
