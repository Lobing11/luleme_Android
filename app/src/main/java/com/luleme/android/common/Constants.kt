package com.luleme.android.common

fun heatLevel(count: Int): Int = when {
    count <= 0 -> 0
    count == 1 -> 1
    count == 2 -> 2
    count == 3 -> 3
    else -> 4
}

object Routes {
    const val HOME = "home"
    const val STATS = "stats"
    const val SETTINGS = "settings"
    const val RECORD_EDIT = "record_edit"
    const val LOCK = "lock"
}

data class RecordEditParam(
    val id: Long? = null,
    val date: Long? = null
)
