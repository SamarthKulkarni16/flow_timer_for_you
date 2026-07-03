package com.flowtimer.app.data

import com.flowtimer.app.SessionRecord
import com.flowtimer.app.SessionRepository
import io.github.jan.supabase.postgrest.from

private const val TABLE = "session_records"

/**
 * Two-way sync between the local Room database and the shared Supabase
 * project. Sync is best-effort: any failure (offline, not signed in, RLS
 * rejection) is swallowed so it never breaks the local-first experience —
 * the app works fully offline and just catches up next time sync runs.
 */
class SyncRepository(
    private val sessionRepository: SessionRepository
) {
    private val auth = SupabaseClientProvider.auth
    private val postgrest = SupabaseClientProvider.postgrest

    /** Push any local sessions created (e.g. offline) that haven't reached the cloud yet. */
    suspend fun pushLocalChanges() {
        val userId = auth.currentUserOrNull()?.id ?: return
        val unsynced = sessionRepository.getUnsyncedSessions()
        if (unsynced.isEmpty()) return

        for (session in unsynced) {
            try {
                postgrest.from(TABLE).upsert(session.toDto(userId))
                sessionRepository.markSynced(session.remoteId)
            } catch (_: Exception) {
                // Leave unsynced; will retry on the next sync pass.
            }
        }
    }

    /** Pull down any sessions that exist in the cloud (e.g. from another device) but not locally. */
    suspend fun pullRemoteChanges() {
        val userId = auth.currentUserOrNull()?.id ?: return
        try {
            val remoteRows = postgrest.from(TABLE)
                .select {
                    filter { eq("user_id", userId) }
                }
                .decodeList<SessionDto>()

            val localIds = sessionRepository.getAllRemoteIds().toSet()
            val newOnes = remoteRows
                .filter { it.id !in localIds }
                .map { it.toSessionRecord() }

            if (newOnes.isNotEmpty()) {
                sessionRepository.insertFromRemote(newOnes)
            }
        } catch (_: Exception) {
            // Offline or RLS issue; app keeps working from local data.
        }
    }

    /** Full sync: push what's new locally, then pull down anything from other devices. */
    suspend fun syncAll() {
        if (auth.currentUserOrNull() == null) return
        pushLocalChanges()
        pullRemoteChanges()
    }
}

private fun SessionRecord.toDto(userId: String) = SessionDto(
    id = remoteId,
    user_id = userId,
    task_name = taskName,
    start_time_millis = startTimeMillis,
    total_allocated_duration_millis = totalAllocatedDurationMillis,
    target_tasks_count = targetTasksCount,
    task_durations_csv = taskDurationsCsv,
    total_actual_duration_millis = totalActualDurationMillis
)

private fun SessionDto.toSessionRecord() = SessionRecord(
    startTimeMillis = start_time_millis,
    totalAllocatedDurationMillis = total_allocated_duration_millis,
    targetTasksCount = target_tasks_count,
    taskDurationsCsv = task_durations_csv,
    totalActualDurationMillis = total_actual_duration_millis,
    taskName = task_name,
    remoteId = id,
    synced = true
)
