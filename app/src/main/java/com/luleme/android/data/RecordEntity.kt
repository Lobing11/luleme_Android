package com.luleme.android.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.luleme.android.model.Record

@Entity(tableName = "records")
data class RecordEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val timestamp: Long,
    val mood: Int,
    val note: String
) {
    fun toModel(): Record = Record(id = id, timestamp = timestamp, mood = mood, note = note)
}
