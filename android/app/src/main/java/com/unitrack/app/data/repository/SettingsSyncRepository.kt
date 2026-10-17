package com.unitrack.app.data.repository

import com.unitrack.app.data.api.SettingsApiService
import com.unitrack.app.data.api.bodyOrThrow
import com.unitrack.app.data.dto.SettingsPatches
import com.unitrack.app.data.local.ExamPeriodPreferences
import com.unitrack.app.data.local.GoalPreferences
import com.unitrack.app.data.local.ThemeMode
import com.unitrack.app.data.local.ThemePreferences
import com.unitrack.app.data.session.tokenOwner
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import java.io.IOException
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

/** Each PATCH changes only its own setting, preserving newer web and other-device edits. */
@Singleton
class SettingsSyncRepository @Inject constructor(
    private val api: SettingsApiService,
    private val json: Json,
    private val themePreferences: ThemePreferences,
    private val goalPreferences: GoalPreferences,
    private val examPeriodPreferences: ExamPeriodPreferences,
    private val authPreferences: AuthPreferences
) {
    suspend fun syncFromServer() {
        val owner = tokenOwner(authPreferences.getAccessTokenOnce())
        val settings = api.getSettings().bodyOrThrow(json)
        if (owner == null || owner != tokenOwner(authPreferences.getAccessTokenOnce())) return

        val mode = settings.themePreference?.let { runCatching { ThemeMode.valueOf(it) }.getOrNull() } ?: ThemeMode.SYSTEM
        themePreferences.setThemeMode(mode)
        goalPreferences.setTargetGpa(settings.targetGpa ?: GoalPreferences.DEFAULT_TARGET_GPA)
        val start = settings.examPeriodStart?.let { runCatching { LocalDate.parse(it.take(10)) }.getOrNull() }
        val end = settings.examPeriodEnd?.let { runCatching { LocalDate.parse(it.take(10)) }.getOrNull() }
        if (start != null && end != null) examPeriodPreferences.setExamPeriod(start, end)
        else examPeriodPreferences.clearExamPeriod()
    }

    suspend fun pushThemeMode(mode: ThemeMode) = pushPatch(SettingsPatches.theme(mode.name))
    suspend fun pushTargetGpa(value: Double) = pushPatch(SettingsPatches.target(value))
    suspend fun pushExamPeriod(start: LocalDate, end: LocalDate) = pushPatch(SettingsPatches.examPeriod(start.toString(), end.toString()))
    suspend fun pushExamPeriodCleared() = pushPatch(SettingsPatches.clearExamPeriod())

    private suspend fun pushPatch(patch: JsonObject) {
        try { api.updateSettings(patch).bodyOrThrow(json) }
        catch (cancelled: CancellationException) { throw cancelled }
        catch (_: IOException) { /* Local preferences remain available offline. */ }
        catch (_: IllegalStateException) { /* Keep local values when synchronization is rejected. */ }
    }
}
