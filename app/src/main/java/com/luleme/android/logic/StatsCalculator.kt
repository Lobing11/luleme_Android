package com.luleme.android.logic

import com.luleme.android.model.Record
import java.util.Calendar

private const val DAY_MS: Long = 24L * 60L * 60L * 1000L

fun startOfDay(ms: Long): Long {
    val d = Calendar.getInstance().apply {
        timeInMillis = ms
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }
    return d.timeInMillis
}

/** month: 0-11 */
fun monthStart(year: Int, month: Int): Long {
    val d = Calendar.getInstance().apply {
        set(Calendar.YEAR, year)
        set(Calendar.MONTH, month)
        set(Calendar.DAY_OF_MONTH, 1)
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }
    return d.timeInMillis
}

/** month: 0-11 */
fun monthEndExclusive(year: Int, month: Int): Long {
    val d = Calendar.getInstance().apply {
        set(Calendar.YEAR, year)
        set(Calendar.MONTH, month)
        set(Calendar.DAY_OF_MONTH, 1)
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
        add(Calendar.MONTH, 1)
    }
    return d.timeInMillis
}

fun recordDays(records: List<Record>): List<Long> {
    val set = LinkedHashSet<Long>()
    for (r in records) {
        set.add(startOfDay(r.timestamp))
    }
    return set.sorted()
}

fun currentStreak(records: List<Record>, now: Long): Int {
    val days = recordDays(records).toHashSet()
    var cursor = startOfDay(now)
    if (!days.contains(cursor)) {
        cursor -= DAY_MS
    }
    var streak = 0
    while (days.contains(cursor)) {
        streak++
        cursor -= DAY_MS
    }
    return streak
}

fun daysSinceLast(records: List<Record>, now: Long): Int? {
    val days = recordDays(records)
    if (days.isEmpty()) return null
    val last = days.last()
    return Math.round((startOfDay(now) - last).toDouble() / DAY_MS).toInt()
}

data class IntervalStats(
    val max: Int,
    val avg: Double,
    val count: Int
)

@Suppress("UNUSED_PARAMETER")
fun intervalStats(records: List<Record>, now: Long): IntervalStats {
    val days = recordDays(records)
    if (days.size < 2) {
        return IntervalStats(max = 0, avg = 0.0, count = 0)
    }
    val gaps = mutableListOf<Int>()
    for (i in 1 until days.size) {
        gaps.add(Math.round((days[i] - days[i - 1]).toDouble() / DAY_MS).toInt())
    }
    var max = 0
    var sum = 0
    for (g in gaps) {
        sum += g
        if (g > max) max = g
    }
    val avg = Math.round((sum.toDouble() / gaps.size) * 10.0) / 10.0
    return IntervalStats(max = max, avg = avg, count = gaps.size)
}

data class MonthCount(
    val year: Int,
    /** 0-11 */
    val month: Int,
    val count: Int
)

fun monthCounts(records: List<Record>, now: Long, months: Int): List<MonthCount> {
    val result = mutableListOf<MonthCount>()
    val cur = Calendar.getInstance().apply { timeInMillis = now }
    val curYear = cur.get(Calendar.YEAR)
    val curMonth = cur.get(Calendar.MONTH)
    for (i in (months - 1) downTo 0) {
        val total = curMonth - i
        val year = curYear + Math.floorDiv(total, 12)
        val month = Math.floorMod(total, 12)
        val start = monthStart(year, month)
        val end = monthEndExclusive(year, month)
        var count = 0
        for (r in records) {
            if (r.timestamp >= start && r.timestamp < end) count++
        }
        result.add(MonthCount(year = year, month = month, count = count))
    }
    return result
}

fun countInMonth(records: List<Record>, year: Int, month: Int): Int {
    val start = monthStart(year, month)
    val end = monthEndExclusive(year, month)
    var count = 0
    for (r in records) {
        if (r.timestamp >= start && r.timestamp < end) count++
    }
    return count
}

fun countToday(records: List<Record>, now: Long): Int {
    val start = startOfDay(now)
    val end = start + DAY_MS
    var count = 0
    for (r in records) {
        if (r.timestamp >= start && r.timestamp < end) count++
    }
    return count
}

fun totalCount(records: List<Record>): Int = records.size

fun countByDayOfMonth(records: List<Record>, year: Int, month: Int): Map<Int, Int> {
    val map = mutableMapOf<Int, Int>()
    val start = monthStart(year, month)
    val end = monthEndExclusive(year, month)
    for (r in records) {
        if (r.timestamp >= start && r.timestamp < end) {
            val day = Calendar.getInstance().apply { timeInMillis = r.timestamp }
                .get(Calendar.DAY_OF_MONTH)
            map[day] = (map[day] ?: 0) + 1
        }
    }
    return map
}

fun recordsInMonth(records: List<Record>, year: Int, month: Int): List<Record> {
    val start = monthStart(year, month)
    val end = monthEndExclusive(year, month)
    return records.filter { it.timestamp >= start && it.timestamp < end }
        .sortedBy { it.timestamp }
}

fun recordsOnDay(records: List<Record>, dayStartMs: Long): List<Record> {
    val end = dayStartMs + DAY_MS
    return records.filter { it.timestamp >= dayStartMs && it.timestamp < end }
        .sortedBy { it.timestamp }
}
