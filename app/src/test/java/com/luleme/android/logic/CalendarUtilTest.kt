package com.luleme.android.logic

import org.junit.Assert.assertEquals
import org.junit.Test

class CalendarUtilTest {

    @Test
    fun june_2025_starts_on_sunday() {
        val grid = buildMonthGrid(2025, 5)
        assertEquals(0, grid.size % 7)
        assertEquals(1, grid[0])
        assertEquals(35, grid.size)
    }

    @Test
    fun february_2024_leap_year() {
        val grid = buildMonthGrid(2024, 1)
        val days = grid.count { it != null }
        assertEquals(29, days)
        assertEquals(0, grid.size % 7)
    }

    @Test
    fun weekday_headers() {
        val headers = weekdayHeaders()
        assertEquals(7, headers.size)
        assertEquals("日", headers[0])
        assertEquals("六", headers[6])
    }

    @Test
    fun shiftMonth_backward_cross_year() {
        val m = shiftMonth(2025, 0, -1)
        assertEquals(2024, m.year)
        assertEquals(11, m.month)
    }

    @Test
    fun shiftMonth_forward_cross_year() {
        val m = shiftMonth(2025, 11, 1)
        assertEquals(2026, m.year)
        assertEquals(0, m.month)
    }
}
