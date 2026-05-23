package com.example.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.SessionRecord
import com.example.data.SessionRepository
import com.example.util.FlowHapticManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

enum class AppScreen {
    Home,
    Setup,
    Timer,
    History
}

class FlowTimerViewModel(private val repository: SessionRepository) : ViewModel() {

    var currentScreen by mutableStateOf(AppScreen.Home)
        private set

    // Setup input fields
    var taskNameInput by mutableStateOf("")
    var totalDurationInput by mutableStateOf("")
    var numberOfTasksInput by mutableStateOf("")

    // Active session state
    var activeTaskName by mutableStateOf("")
    var totalTasksCount by mutableStateOf(1)
    var currentTaskIndex by mutableStateOf(0)

    var totalDurationSeconds by mutableStateOf(0L)
    var timerRemainingSeconds by mutableStateOf(0L)
    var timerTextState by mutableStateOf("00:00")
        private set

    var isDoneState by mutableStateOf(false)
        private set

    // History data
    val allSessions: StateFlow<List<SessionRecord>> = repository.allSessions
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    private val taskRealDurations = mutableListOf<Long>()
    private var sessionStartTimeMillis = 0L
    private var taskStartRealTimeMillis = 0L
    private var timerJob: Job? = null

    // Time Formatting buffer to avoid allocations in updates/loops
    private val timeCharBuf = CharArray(5) { '0' }.apply { this[2] = ':' }

    fun navigateTo(screen: AppScreen) {
        currentScreen = screen
    }

    private fun formatTimeSeconds(seconds: Long): String {
        val secs = seconds.coerceAtLeast(0L)
        val m = (secs / 60) % 100
        val s = secs % 60
        timeCharBuf[0] = ('0'.code + (m / 10).toInt()).toChar()
        timeCharBuf[1] = ('0'.code + (m % 10).toInt()).toChar()
        timeCharBuf[3] = ('0'.code + (s / 10).toInt()).toChar()
        timeCharBuf[4] = ('0'.code + (s % 10).toInt()).toChar()
        return String(timeCharBuf)
    }

    fun startSession() {
        // Parse fields on start
        val cleanName = taskNameInput.trim().ifEmpty { "task" }
        val durationMillis = parseMmSsToMillis(totalDurationInput.trim()) ?: (20 * 60 * 1000L) // Default 20 mins
        val taskCount = numberOfTasksInput.toIntOrNull()?.coerceAtLeast(1) ?: 10 // Default 10 tasks

        activeTaskName = cleanName
        totalTasksCount = taskCount
        currentTaskIndex = 0
        val parsedSeconds = durationMillis / 1000L
        // Ensure total duration is at least taskCount seconds, so each task gets at least 1 second
        totalDurationSeconds = parsedSeconds.coerceAtLeast(taskCount.toLong())
        timerRemainingSeconds = totalDurationSeconds
        timerTextState = formatTimeSeconds(timerRemainingSeconds)

        taskRealDurations.clear()
        sessionStartTimeMillis = System.currentTimeMillis()
        taskStartRealTimeMillis = System.currentTimeMillis()
        isDoneState = false

        navigateTo(AppScreen.Timer)
        startTimerTicker()
    }

    private fun startTimerTicker() {
        timerJob?.cancel()
        val allocatedSecondsPerTask = totalDurationSeconds / totalTasksCount

        timerJob = viewModelScope.launch {
            while (isActive && timerRemainingSeconds > 0) {
                delay(1000L)
                if (timerRemainingSeconds > 0) {
                    timerRemainingSeconds -= 1
                    timerTextState = formatTimeSeconds(timerRemainingSeconds)
                }

                val normalTaskEndTime = totalDurationSeconds - (currentTaskIndex + 1) * allocatedSecondsPerTask
                if (timerRemainingSeconds <= normalTaskEndTime) {
                    completeCurrentTask(expiredNaturally = true)
                }
            }
        }
    }

    fun completeCurrentTask(expiredNaturally: Boolean) {
        // Guard against double clicks/race conditions during state transition
        if (isDoneState || currentTaskIndex >= totalTasksCount) return

        if (!expiredNaturally) {
            FlowHapticManager.vibrate(500)
        }

        val now = System.currentTimeMillis()
        val actualTaskDurationMillis = now - taskStartRealTimeMillis
        taskRealDurations.add(actualTaskDurationMillis)

        val nextIndex = currentTaskIndex + 1

        if (nextIndex >= totalTasksCount) {
            timerJob?.cancel()
            timerRemainingSeconds = 0
            timerTextState = "00:00"
            currentTaskIndex = nextIndex
            saveSessionAndShowDone()
        } else {
            currentTaskIndex = nextIndex
            val allocatedSecondsPerTask = totalDurationSeconds / totalTasksCount
            val nextTaskStartTimeNormal = totalDurationSeconds - nextIndex * allocatedSecondsPerTask
            timerRemainingSeconds = nextTaskStartTimeNormal
            timerTextState = formatTimeSeconds(timerRemainingSeconds)
            taskStartRealTimeMillis = System.currentTimeMillis()
        }
    }

    private fun saveSessionAndShowDone() {
        isDoneState = true

        val record = SessionRecord(
            startTimeMillis = sessionStartTimeMillis,
            totalAllocatedDurationMillis = totalDurationSeconds * 1000L,
            targetTasksCount = totalTasksCount,
            taskDurationsCsv = SessionRecord.createCsv(taskRealDurations),
            totalActualDurationMillis = System.currentTimeMillis() - sessionStartTimeMillis
        )

        viewModelScope.launch(Dispatchers.IO) {
            repository.insertSession(record)
        }

        viewModelScope.launch {
            delay(5000L)
            isDoneState = false
            navigateTo(AppScreen.Home)
        }
    }

    private fun parseMmSsToMillis(input: String): Long? {
        val clean = input.trim().lowercase(java.util.Locale.US)
        if (clean.isEmpty()) return null

        // If it contains a colon, parse as MM:SS
        if (clean.contains(":")) {
            val parts = clean.split(":")
            if (parts.isEmpty()) return null
            val minsStr = parts[0].filter { it.isDigit() }
            val minutes = minsStr.toLongOrNull() ?: 0L
            val seconds = if (parts.size > 1) {
                val secsStr = parts[1].filter { it.isDigit() }
                secsStr.toLongOrNull() ?: 0L
            } else {
                0L
            }
            if (minutes < 0 || seconds < 0 || seconds >= 60) return null
            return (minutes * 60 + seconds) * 1000L
        }

        // Just numeric digits or digits with suffixes (e.g. "30", "30m", "30 mins")
        val onlyDigits = clean.filter { it.isDigit() }
        val minutes = onlyDigits.toLongOrNull()
        if (minutes != null && minutes > 0L) {
            return minutes * 60 * 1000L
        }
        return null
    }
}

class FlowTimerViewModelFactory(private val repository: SessionRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(FlowTimerViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return FlowTimerViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
