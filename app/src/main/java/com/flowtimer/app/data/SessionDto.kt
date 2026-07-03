package com.flowtimer.app.data

import kotlinx.serialization.Serializable

/**
 * Row shape for the `session_records` table in Supabase. Field names match
 * the SQL columns exactly (see supabase_setup.sql at the repo root).
 */
@Serializable
data class SessionDto(
    val id: String,
    val user_id: String,
    val task_name: String,
    val start_time_millis: Long,
    val total_allocated_duration_millis: Long,
    val target_tasks_count: Int,
    val task_durations_csv: String,
    val total_actual_duration_millis: Long
)
