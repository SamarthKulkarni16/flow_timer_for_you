package com.example.data

import kotlinx.coroutines.flow.Flow

class SessionRepository(private val sessionDao: SessionDao) {
    val allSessions: Flow<List<SessionRecord>> = sessionDao.getAllSessions()

    suspend fun insertSession(session: SessionRecord) {
        sessionDao.insertSession(session)
    }
}
