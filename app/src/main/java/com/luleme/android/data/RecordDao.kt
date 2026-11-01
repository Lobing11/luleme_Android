package com.luleme.android.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface RecordDao {
    @Insert
    suspend fun insert(entity: RecordEntity): Long

    @Update
    suspend fun update(entity: RecordEntity)

    @Query("DELETE FROM records WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM records")
    suspend fun clearAll()

    @Query("SELECT * FROM records ORDER BY timestamp DESC")
    suspend fun getAll(): List<RecordEntity>

    @Query("SELECT * FROM records ORDER BY timestamp DESC")
    fun observeAll(): Flow<List<RecordEntity>>

    @Query("SELECT * FROM records WHERE id = :id LIMIT 1")
    suspend fun getById(id: Long): RecordEntity?
}
