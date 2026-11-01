package com.luleme.android.model

import java.util.Calendar
import java.util.Locale

data class Record(
    val id: Long,
    val timestamp: Long,
    val mood: Int,
    val note: String
)

data class NewRecord(
    val timestamp: Long,
    val mood: Int,
    val note: String
)

data class MoodOption(
    val value: Int,
    val emoji: String,
    val label: String
)

val MOODS: List<MoodOption> = listOf(
    MoodOption(0, "🥰", "愉悦"),
    MoodOption(1, "😌", "放松"),
    MoodOption(2, "😄", "兴奋"),
    MoodOption(3, "😴", "疲惫"),
    MoodOption(4, "😕", "一般")
)

const val DEFAULT_MOOD: Int = 2

fun moodEmoji(value: Int): String =
    MOODS.firstOrNull { it.value == value }?.emoji ?: "😄"

fun moodLabel(value: Int): String =
    MOODS.firstOrNull { it.value == value }?.label ?: ""

@Suppress("UNCHECKED_CAST")
fun parseRecord(raw: Map<String, Any?>): Record? {
    val id = (raw["id"] as? Number)?.toLong() ?: return null
    val timestamp = (raw["timestamp"] as? Number)?.toLong() ?: return null
    val mood = (raw["mood"] as? Number)?.toInt() ?: return null
    if (mood < 0 || mood > 4) return null
    val note = raw["note"] as? String ?: return null
    return Record(id = id, timestamp = timestamp, mood = mood, note = note)
}

fun formatDateTime(ts: Long): String {
    val cal = Calendar.getInstance().apply { timeInMillis = ts }
    val mm = String.format(Locale.US, "%02d", cal.get(Calendar.MONTH) + 1)
    val dd = String.format(Locale.US, "%02d", cal.get(Calendar.DAY_OF_MONTH))
    val hh = String.format(Locale.US, "%02d", cal.get(Calendar.HOUR_OF_DAY))
    val mi = String.format(Locale.US, "%02d", cal.get(Calendar.MINUTE))
    return "$mm-$dd $hh:$mi"
}
