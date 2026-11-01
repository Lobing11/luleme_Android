package com.luleme.android.logic

import java.util.Calendar

private val WEEKDAYS: List<String> = listOf("日", "一", "二", "三", "四", "五", "六")

fun weekdayHeaders(): List<String> = WEEKDAYS

/**
 * month: 0-11
 * null cells are placeholders for adjacent months.
 */
fun buildMonthGrid(year: Int, month: Int): List<Int?> {
    val first = Calendar.getInstance().apply {
        set(Calendar.YEAR, year)
        set(Calendar.MONTH, month)
        set(Calendar.DAY_OF_MONTH, 1)
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }
    val startWeekday = first.get(Calendar.DAY_OF_WEEK) - Calendar.SUNDAY
    val daysInMonth = first.getActualMaximum(Calendar.DAY_OF_MONTH)
    val cells = mutableListOf<Int?>()
    repeat(startWeekday) { cells.add(null) }
    for (d in 1..daysInMonth) {
        cells.add(d)
    }
    while (cells.size % 7 != 0) {
        cells.add(null)
    }
    return cells
}

data class YearMonth(val year: Int, val month: Int)

fun shiftMonth(year: Int, month: Int, delta: Int): YearMonth {
    val total = month + delta
    val y = year + Math.floorDiv(total, 12)
    val m = Math.floorMod(total, 12)
    return YearMonth(year = y, month = m)
}
