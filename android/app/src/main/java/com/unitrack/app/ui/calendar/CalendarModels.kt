package com.unitrack.app.ui.calendar

import androidx.compose.ui.graphics.Color

/**
 * Backend'deki (calendar.types.ts) EventType/Priority/Status/Recurrence
 * string sabitleriyle birebir eşleşen enum'lar + Türkçe etiketler.
 *
 * Bilinçli olarak backend'deki ham string değerler ("lesson", "exam" ...)
 * kullanılıyor; DTO'lar da bu string'leri taşıyor. Enum sadece UI tarafında,
 * dropdown/seçim listeleri ve Türkçe metin/ikon eşlemesi için var.
 */
enum class EventTypeUi(val apiValue: String, val label: String) {
    LESSON("lesson", "Ders"),
    EXAM("exam", "Sınav"),
    QUIZ("quiz", "Quiz"),
    ASSIGNMENT("assignment", "Ödev"),
    PROJECT("project", "Proje"),
    PRESENTATION("presentation", "Sunum"),
    OTHER("other", "Diğer");

    companion object {
        fun fromApiValue(value: String): EventTypeUi =
            entries.find { it.apiValue == value } ?: OTHER
    }
}

enum class EventPriorityUi(val apiValue: String, val label: String) {
    LOW("low", "Düşük"),
    MEDIUM("medium", "Orta"),
    HIGH("high", "Yüksek");

    companion object {
        fun fromApiValue(value: String): EventPriorityUi =
            entries.find { it.apiValue == value } ?: MEDIUM
    }
}

enum class EventStatusUi(val apiValue: String, val label: String) {
    PENDING("pending", "Bekliyor"),
    IN_PROGRESS("in_progress", "Devam Ediyor"),
    COMPLETED("completed", "Tamamlandı"),
    CANCELLED("cancelled", "İptal");

    companion object {
        fun fromApiValue(value: String): EventStatusUi =
            entries.find { it.apiValue == value } ?: PENDING
    }
}

enum class EventRecurrenceUi(val apiValue: String, val label: String) {
    NONE("none", "Yok"),
    DAILY("daily", "Günlük"),
    WEEKLY("weekly", "Haftalık"),
    MONTHLY("monthly", "Aylık");

    companion object {
        fun fromApiValue(value: String): EventRecurrenceUi =
            entries.find { it.apiValue == value } ?: NONE
    }
}

enum class CalendarViewMode(val label: String) {
    MONTH("Ay"),
    WEEK("Hafta"),
    LIST("Liste"),
    UPCOMING("Yaklaşan")
}

/** Etkinlik oluştururken seçilebilecek küçük, önceden tanımlı renk paleti. */
val EventColorPalette = listOf(
    "#6366F1", // indigo (varsayılan)
    "#EF4444", // kırmızı
    "#F59E0B", // amber
    "#10B981", // yeşil
    "#3B82F6", // mavi
    "#8B5CF6", // mor
    "#EC4899", // pembe
    "#06B6D4"  // camgöbeği
)

fun parseEventColor(hex: String): Color = try {
    Color(android.graphics.Color.parseColor(hex))
} catch (e: Exception) {
    Color(0xFF6366F1)
}

/** Kullanıcının "N gün/saat/dakika önce" olarak okuyacağı kısa özet. */
fun formatNotificationOffset(daysBefore: Int, hoursBefore: Int, minutesBefore: Int): String {
    val parts = mutableListOf<String>()
    if (daysBefore > 0) parts += "$daysBefore gün"
    if (hoursBefore > 0) parts += "$hoursBefore saat"
    if (minutesBefore > 0) parts += "$minutesBefore dakika"
    return if (parts.isEmpty()) "Etkinlik anında" else parts.joinToString(" ") + " önce"
}

/** Haftanın günü (backend: 0=Pazar..6=Cumartesi) için kısa Türkçe etiket. */
fun dayOfWeekLabel(dayOfWeek: Int): String = when (dayOfWeek) {
    0 -> "Pazar"
    1 -> "Pazartesi"
    2 -> "Salı"
    3 -> "Çarşamba"
    4 -> "Perşembe"
    5 -> "Cuma"
    6 -> "Cumartesi"
    else -> "-"
}

fun dayOfWeekShortLabel(dayOfWeek: Int): String = when (dayOfWeek) {
    0 -> "Paz"
    1 -> "Pzt"
    2 -> "Sal"
    3 -> "Çar"
    4 -> "Per"
    5 -> "Cum"
    6 -> "Cmt"
    else -> "-"
}

/**
 * java.time.DayOfWeek (MONDAY=1..SUNDAY=7) ile backend'in JS `Date.getDay()`
 * kuralı (SUNDAY=0..SATURDAY=6) arasında dönüşüm. course_schedule.dayOfWeek
 * hep JS kuralında saklanıyor, ekranda LocalDate kullanırken bu köprü gerekli.
 */
fun java.time.DayOfWeek.toBackendDayOfWeek(): Int =
    if (this == java.time.DayOfWeek.SUNDAY) 0 else this.value

