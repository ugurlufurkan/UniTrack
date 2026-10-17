package com.unitrack.app.data.dto

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

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

internal object SettingsPatches {
    fun theme(value: String) = buildJsonObject { put("themePreference", value) }
    fun target(value: Double) = buildJsonObject { put("targetGpa", value) }
    fun examPeriod(start: String, end: String) = buildJsonObject { put("examPeriodStart", start); put("examPeriodEnd", end) }
    fun clearExamPeriod() = buildJsonObject { put("examPeriodStart", JsonNull); put("examPeriodEnd", JsonNull) }
}
