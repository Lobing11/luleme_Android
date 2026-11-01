package com.luleme.android.data

import android.content.Context
import com.luleme.android.model.Record
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class RecordRepository private constructor(private val dao: RecordDao) {

    fun observeAll(): Flow<List<Record>> =
        dao.observeAll().map { list -> list.map { it.toModel() } }

    suspend fun getAll(): List<Record> = dao.getAll().map { it.toModel() }

    suspend fun getById(id: Long): Record? = dao.getById(id)?.toModel()

    suspend fun add(timestamp: Long, mood: Int, note: String): Long {
        return dao.insert(RecordEntity(timestamp = timestamp, mood = mood, note = note))
    }

    suspend fun update(id: Long, timestamp: Long, mood: Int, note: String) {
        dao.update(RecordEntity(id = id, timestamp = timestamp, mood = mood, note = note))
    }

    suspend fun remove(id: Long) {
        dao.deleteById(id)
    }

    suspend fun clearAll() {
        dao.clearAll()
    }

    companion object {
        @Volatile
        private var instance: RecordRepository? = null

        fun getInstance(context: Context): RecordRepository {
            return instance ?: synchronized(this) {
                instance ?: RecordRepository(
                    AppDatabase.getInstance(context).recordDao()
                ).also { instance = it }
            }
        }
    }
}
