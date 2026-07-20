package com.unitrack.app.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback

/**
 * Uygulama genelinde tutarlı dokunsal geri bildirim için tek noktadan
 * çağrılabilen küçük yardımcı. Compose'un LocalHapticFeedback'ini sarmalar,
 * her ekranın kendi HapticFeedbackType sabitini seçmesiyle uğraşmasını
 * engeller.
 *
 * Kullanım:
 *   val haptic = LocalHapticFeedback.current
 *   Button(onClick = { haptic.click(); viewModel.save() }) { ... }
 */
fun HapticFeedback.click() {
    performHapticFeedback(HapticFeedbackType.LongPress)
}

fun HapticFeedback.refresh() {
    performHapticFeedback(HapticFeedbackType.TextHandleMove)
}

/** Composable içinde `val haptic = rememberHaptic()` ile kısa kullanım. */
@Composable
fun rememberHaptic(): HapticFeedback = LocalHapticFeedback.current
