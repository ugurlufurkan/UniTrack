package com.unitrack.app.data.dto

import kotlinx.serialization.Serializable

// ==========================
// SETTINGS (cihazlar arası senkron edilen kişisel ayarlar)
// GET/PATCH /api/settings
// ==========================

@Serializable
data class SettingsDto(
    val themePreference: String? = null, // SYSTEM | LIGHT | DARK
    val targetGpa: Double? = null,
    val examPeriodStart: String? = null, // ISO-8601
    val examPeriodEnd: String? = null
)

/**
 * PATCH gövdesi: alan hiç eklenmezse dokunulmaz, null gönderilirse o ayar
 * sıfırlanır. kotlinx.serialization varsayılan olarak `null` alanları da
 * gövdeye yazdığı için (encodeDefaults açık olmasa da açıkça atanmış null
 * alanlar serialize edilir), tek seferde sadece değişen alan(lar)ı
 * göndermek istersen ayrı ayrı PATCH çağrıları at.
 */
@Serializable
data class SettingsUpdateRequest(
    val themePreference: String? = null,
    val targetGpa: Double? = null,
    val examPeriodStart: String? = null,
    val examPeriodEnd: String? = null
)
