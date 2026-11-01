package com.luleme.android.logic

import com.luleme.android.model.Record
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar

class StatsCalculatorTest {

    private fun rec(ts: Long): Record = Record(id = 0, timestamp = ts, mood = 2, note = "")

    private fun at(y: Int, m: Int, d: Int, h: Int = 10): Long {
        return Calendar.getInstance().apply {
            set(Calendar.YEAR, y)
            set(Calendar.MONTH, m)
            set(Calendar.DAY_OF_MONTH, d)
            set(Calendar.HOUR_OF_DAY, h)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
    }

    @Test
    fun currentStreak_today_and_yesterday() {
        val now = at(2025, 5, 15)
        val records = listOf(rec(at(2025, 5, 15, 9)), rec(at(2025, 5, 14, 22)), rec(at(2025, 5, 12, 8)))
        assertEquals(2, currentStreak(records, now))
    }

    @Test
    fun currentStreak_today_missing() {
        val now = at(2025, 5, 15)
        val records = listOf(rec(at(2025, 5, 14, 20)), rec(at(2025, 5, 13, 8)))
        assertEquals(2, currentStreak(records, now))
    }

    @Test
    fun currentStreak_no_recent() {
        val now = at(2025, 5, 15)
        assertEquals(0, currentStreak(listOf(rec(at(2025, 5, 10))), now))
    }

    @Test
    fun currentStreak_empty() {
        assertEquals(0, currentStreak(emptyList(), at(2025, 5, 15)))
    }

    @Test
    fun daysSinceLast_gap_two() {
        assertEquals(2, daysSinceLast(listOf(rec(at(2025, 5, 13))), at(2025, 5, 15)))
    }

    @Test
    fun daysSinceLast_today() {
        assertEquals(0, daysSinceLast(listOf(rec(at(2025, 5, 15, 8))), at(2025, 5, 15)))
    }

    @Test
    fun daysSinceLast_empty() {
        assertNull(daysSinceLast(emptyList(), at(2025, 5, 15)))
    }

    @Test
    fun intervalStats_gaps() {
        val records = listOf(rec(at(2025, 6, 1)), rec(at(2025, 6, 5)), rec(at(2025, 6, 10)))
        val s = intervalStats(records, at(2025, 6, 15))
        assertEquals(5, s.max)
        assertEquals(4.5, s.avg, 0.001)
        assertEquals(2, s.count)
    }

    @Test
    fun intervalStats_single() {
        val s = intervalStats(listOf(rec(at(2025, 6, 1))), at(2025, 6, 15))
        assertEquals(0, s.max)
        assertEquals(0, s.count)
    }

    @Test
    fun monthCounts_six_month_window() {
        // now = 2025-06；窗口 Jan..Jun（0-index month）
        val now = at(2025, 5, 15)
        val records = listOf(
            rec(at(2025, 5, 2)),
            rec(at(2025, 5, 10)),
            rec(at(2025, 4, 1)),
            rec(at(2025, 1, 20)),
            rec(at(2025, 2, 3))
        )
        val counts = monthCounts(records, now, 6)
        assertEquals(6, counts.size)
        assertEquals(0, counts[0].month)
        assertEquals(2025, counts[0].year)
        assertEquals(1, counts[1].count)
        assertEquals(2025, counts[5].year)
        assertEquals(5, counts[5].month)
        assertEquals(2, counts[5].count)
        assertEquals(1, counts[4].count)
    }

    @Test
    fun monthCounts_cross_year() {
        val now = at(2025, 0, 15)
        val counts = monthCounts(listOf(rec(at(2024, 11, 5))), now, 3)
        assertEquals(2024, counts[0].year)
        assertEquals(10, counts[0].month)
        assertEquals(0, counts[0].count)
        assertEquals(11, counts[1].month)
        assertEquals(1, counts[1].count)
    }

    @Test
    fun countInMonth_filters() {
        val records = listOf(
            rec(at(2025, 5, 30)),
            rec(at(2025, 6, 1)),
            rec(at(2025, 6, 30, 23)),
            rec(at(2025, 7, 1))
        )
        // month 5 = June → 1 条；month 6 = July → 2 条
        assertEquals(1, countInMonth(records, 2025, 5))
        assertEquals(2, countInMonth(records, 2025, 6))
        assertEquals(0, countInMonth(records, 2025, 4))
    }

    @Test
    fun countToday_only_today() {
        val now = at(2025, 6, 15, 18)
        val records = listOf(rec(at(2025, 6, 15, 8)), rec(at(2025, 6, 15, 23)), rec(at(2025, 6, 14, 23)))
        assertEquals(2, countToday(records, now))
    }

    @Test
    fun recordsOnDay_filters() {
        val records = listOf(rec(at(2025, 6, 15, 8)), rec(at(2025, 6, 15, 20)), rec(at(2025, 6, 16, 1)))
        val list = recordsOnDay(records, startOfDay(at(2025, 6, 15)))
        assertEquals(2, list.size)
        assertTrue(list[0].timestamp < list[1].timestamp)
    }

    @Test
    fun countByDayOfMonth_maps() {
        // month 6 = July（与 at() 的 0-based 月份一致）
        val records = listOf(rec(at(2025, 6, 3, 9)), rec(at(2025, 6, 3, 21)), rec(at(2025, 6, 15)))
        val map = countByDayOfMonth(records, 2025, 6)
        assertEquals(2, map[3])
        assertEquals(1, map[15])
        assertNull(map[10])
    }

    @Test
    fun recordDays_dedupes() {
        val days = recordDays(
            listOf(rec(at(2025, 6, 15, 8)), rec(at(2025, 6, 14, 9)), rec(at(2025, 6, 15, 22)))
        )
        assertEquals(2, days.size)
        assertTrue(days[0] < days[1])
    }

    @Test
    fun heatLevel_mapping() {
        assertEquals(0, com.luleme.android.common.heatLevel(0))
        assertEquals(1, com.luleme.android.common.heatLevel(1))
        assertEquals(2, com.luleme.android.common.heatLevel(2))
        assertEquals(3, com.luleme.android.common.heatLevel(3))
        assertEquals(4, com.luleme.android.common.heatLevel(4))
        assertEquals(4, com.luleme.android.common.heatLevel(9))
    }

    @Test
    fun geshenFireTime_quiet_hours() {
        // 22:00 + 8h = 06:00 next day -> quiet -> 08:00
        val record = at(2025, 5, 15, 22)
        val fire = ReminderScheduler.geshenFireTime(record)
        val cal = Calendar.getInstance().apply { timeInMillis = fire }
        assertEquals(8, cal.get(Calendar.HOUR_OF_DAY))
        assertEquals(0, cal.get(Calendar.MINUTE))
    }
}
