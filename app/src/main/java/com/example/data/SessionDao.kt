package com.example.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface SessionDao {
    @Query("SELECT * FROM session_records ORDER BY startTimeMillis DESC")
    fun getAllSessions(): Flow<List<SessionRecord>>

    @Insert
    suspend fun insertSession(session: SessionRecord)
}
