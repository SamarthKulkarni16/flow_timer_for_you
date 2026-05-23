package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "session_records")
data class SessionRecord(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val startTimeMillis: Long,
    val totalAllocatedDurationMillis: Long,
    val targetTasksCount: Int,
    val taskDurationsCsv: String, // durations in Milliseconds of each task completed, separated by commas
    val totalActualDurationMillis: Long
) {
    fun getTaskDurationsList(): List<Long> {
        if (taskDurationsCsv.isEmpty()) return emptyList()
        return taskDurationsCsv.split(",").mapNotNull { it.toLongOrNull() }
    }

    companion object {
        fun createCsv(durations: List<Long>): String {
            return durations.joinToString(",")
        }
    }
}
