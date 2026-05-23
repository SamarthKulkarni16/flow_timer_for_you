package com.example.ui

import android.app.Activity
import android.os.Build
import android.view.WindowInsetsController
import android.view.WindowManager
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.SessionRecord
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun FlowTimerApp(viewModel: FlowTimerViewModel) {
    val context = LocalContext.current
    val activity = context as? Activity

    // Global back handling
    BackHandler {
        when (viewModel.currentScreen) {
            AppScreen.Setup -> viewModel.navigateTo(AppScreen.Home)
            AppScreen.Timer -> viewModel.navigateTo(AppScreen.Home)
            AppScreen.History -> viewModel.navigateTo(AppScreen.Home)
            AppScreen.Home -> activity?.finish()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        when (viewModel.currentScreen) {
            AppScreen.Home -> HomeScreen(
                timerText = "00:00",
                onStartClicked = { viewModel.navigateTo(AppScreen.Setup) },
                onTripleTap = { viewModel.navigateTo(AppScreen.History) }
            )
            AppScreen.Setup -> SetupScreen(
                taskName = viewModel.taskNameInput,
                onTaskNameChange = { viewModel.taskNameInput = it },
                totalDuration = viewModel.totalDurationInput,
                onTotalDurationChange = { viewModel.totalDurationInput = it },
                numberOfTasks = viewModel.numberOfTasksInput,
                onNumberOfTasksChange = { viewModel.numberOfTasksInput = it },
                onStartClicked = { viewModel.startSession() }
            )
            AppScreen.Timer -> TimerScreen(viewModel = viewModel)
            AppScreen.History -> {
                val sessions by viewModel.allSessions.collectAsStateWithLifecycle()
                HistoryScreen(
                    sessions = sessions,
                    onBack = { viewModel.navigateTo(AppScreen.Home) }
                )
            }
        }
    }
}

@Composable
fun HomeScreen(
    timerText: String,
    onStartClicked: () -> Unit,
    onTripleTap: () -> Unit
) {
    var lastTapTime by remember { mutableLongStateOf(0L) }
    var tapCount by remember { mutableIntStateOf(0) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectTapGestures(
                    onTap = {
                        val currentTime = System.currentTimeMillis()
                        if (currentTime - lastTapTime < 450L) {
                            tapCount++
                            if (tapCount >= 3) {
                                tapCount = 0
                                onTripleTap()
                            }
                        } else {
                            tapCount = 1
                        }
                        lastTapTime = currentTime
                    }
                )
            }
            .windowInsetsPadding(WindowInsets.safeDrawing),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.size(280.dp)
        ) {
            Canvas(modifier = Modifier.fillMaxSize().testTag("home_timer_circle")) {
                val strokeWidth = 8.dp.toPx()
                val radius = ((size.minDimension - strokeWidth) / 2f).coerceAtLeast(0f)
                drawCircle(
                    color = Color.White,
                    radius = radius,
                    style = Stroke(width = strokeWidth)
                )
            }
            Text(
                text = timerText,
                color = Color.White,
                fontSize = 58.sp,
                fontFamily = FontFamily.SansSerif,
                fontWeight = FontWeight.Normal,
                textAlign = TextAlign.Center
            )
        }

        Spacer(modifier = Modifier.height(48.dp))

        Text(
            text = "start",
            color = Color.White,
            fontSize = 22.sp,
            fontFamily = FontFamily.SansSerif,
            fontWeight = FontWeight.Light,
            modifier = Modifier
                .testTag("start_button")
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) {
                    onStartClicked()
                }
                .padding(24.dp)
        )
    }
}

@Composable
fun SetupScreen(
    taskName: String,
    onTaskNameChange: (String) -> Unit,
    totalDuration: String,
    onTotalDurationChange: (String) -> Unit,
    numberOfTasks: String,
    onNumberOfTasksChange: (String) -> Unit,
    onStartClicked: () -> Unit
) {
    val focusManager = LocalFocusManager.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .padding(horizontal = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // Top dummy padding for perfect aesthetic balance
        Spacer(modifier = Modifier.height(32.dp))

        // Three vertically stacked inputs in the center
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.weight(1f)
        ) {
            // Task Name input
            MinimalInputField(
                value = taskName,
                onValueChange = onTaskNameChange,
                keyboardType = KeyboardType.Text,
                imeAction = ImeAction.Next,
                onImeAction = { focusManager.moveFocus(androidx.compose.ui.focus.FocusDirection.Down) },
                testTag = "input_task_name"
            )

            Spacer(modifier = Modifier.height(40.dp))

            // Total Duration Input
            MinimalInputField(
                value = totalDuration,
                onValueChange = onTotalDurationChange,
                keyboardType = KeyboardType.Number,
                imeAction = ImeAction.Next,
                onImeAction = { focusManager.moveFocus(androidx.compose.ui.focus.FocusDirection.Down) },
                testTag = "input_total_duration"
            )

            Spacer(modifier = Modifier.height(40.dp))

            // Number of Tasks Input
            MinimalInputField(
                value = numberOfTasks,
                onValueChange = onNumberOfTasksChange,
                keyboardType = KeyboardType.Number,
                imeAction = ImeAction.Done,
                onImeAction = {
                    focusManager.clearFocus()
                    onStartClicked()
                },
                testTag = "input_number_tasks"
            )
        }

        // Action button at bottom
        Text(
            text = "start",
            color = Color.White,
            fontSize = 22.sp,
            fontFamily = FontFamily.SansSerif,
            fontWeight = FontWeight.Light,
            modifier = Modifier
                .testTag("setup_start_button")
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) {
                    onStartClicked()
                }
                .padding(24.dp)
        )
    }
}

@Composable
fun MinimalInputField(
    value: String,
    onValueChange: (String) -> Unit,
    keyboardType: KeyboardType,
    imeAction: ImeAction,
    onImeAction: () -> Unit,
    testTag: String
) {
    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        textStyle = TextStyle(
            color = Color.White,
            fontSize = 32.sp,
            fontFamily = FontFamily.SansSerif,
            textAlign = TextAlign.Center
        ),
        keyboardOptions = KeyboardOptions(
            keyboardType = keyboardType,
            imeAction = imeAction
        ),
        keyboardActions = KeyboardActions(
            onAny = { onImeAction() }
        ),
        cursorBrush = SolidColor(Color.White),
        modifier = Modifier
            .testTag(testTag)
            .width(280.dp)
            .padding(vertical = 12.dp),
        decorationBox = { innerTextField ->
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                innerTextField()
                Spacer(modifier = Modifier.height(8.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(2.dp)
                        .background(Color.White)
                )
            }
        }
    )
}

@Composable
fun TimerScreen(viewModel: FlowTimerViewModel) {
    val context = LocalContext.current
    val activity = context as? Activity

    // Absolute Window Immersion (Status bar and Navigation bar complete hiding) using safe, official Jetpack Compat wrappers
    DisposableEffect(activity) {
        val window = activity?.window
        if (window != null) {
            // Keep Screen Awake
            window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

            // Hide system navigation and status bars completely and safely on all API levels
            try {
                WindowCompat.setDecorFitsSystemWindows(window, false)
                val controller = WindowInsetsControllerCompat(window, window.decorView)
                controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
                controller.hide(WindowInsetsCompat.Type.systemBars())
            } catch (e: Exception) {
                // Fallback graceful check to prevent crash on custom Android builds / ROMs
            }
        }

        onDispose {
            if (window != null) {
                try {
                    // Clear keep screen awake flag
                    window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

                    // Restore system navigation and status bars safely
                    WindowCompat.setDecorFitsSystemWindows(window, true)
                    val controller = WindowInsetsControllerCompat(window, window.decorView)
                    controller.show(WindowInsetsCompat.Type.systemBars())
                } catch (e: Exception) {
                    // Fallback to avoid any crash during activity destruction or unbinding
                }
            }
        }
    }

    if (viewModel.isDoneState) {
        // Completion state: Screen stays pure black with "Done." exactly in the middle
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "Done.",
                color = Color.White,
                fontSize = 32.sp,
                fontFamily = FontFamily.SansSerif,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.testTag("done_text")
            )
        }
    } else {
        // Immersive active flow timer layout
        val progressRatio = if (viewModel.totalDurationSeconds > 0) {
            viewModel.timerRemainingSeconds.toFloat() / viewModel.totalDurationSeconds.toFloat()
        } else {
            0f
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    detectTapGestures(
                        onTap = {
                            viewModel.completeCurrentTask(expiredNaturally = false)
                        }
                    )
                }
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            // Top mid-section container
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier.fillMaxSize()
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.size(280.dp)
                ) {
                    // Sweeping arc surrounds the timer text (radial progress depletion)
                    Canvas(modifier = Modifier.fillMaxSize().testTag("active_timer_circle")) {
                        if (progressRatio > 0f) {
                            val strokeWidth = 8.dp.toPx() // thick circle outline
                            val sizeMin = size.minDimension
                            val diameter = sizeMin - strokeWidth
                            val topLeftX = (size.width - diameter) / 2f
                            val topLeftY = (size.height - diameter) / 2f
                            val sweepAngle = 360f * progressRatio

                            drawArc(
                                color = Color.White,
                                startAngle = -90f, // Deplete/Start clockwise from top mid-section
                                sweepAngle = sweepAngle,
                                useCenter = false,
                                topLeft = androidx.compose.ui.geometry.Offset(topLeftX, topLeftY),
                                size = androidx.compose.ui.geometry.Size(diameter, diameter),
                                style = Stroke(width = strokeWidth)
                            )
                        }
                    }

                    // Display big countdown timer text
                    Text(
                        text = viewModel.timerTextState,
                        color = Color.White,
                        fontSize = 58.sp,
                        fontFamily = FontFamily.SansSerif,
                        fontWeight = FontWeight.Normal,
                        textAlign = TextAlign.Center
                    )
                }

                Spacer(modifier = Modifier.height(72.dp))

                // The customized Task Label Progress showing only the task count progress and not the custom task name
                val activeTaskNum = viewModel.currentTaskIndex + 1
                Text(
                    text = "$activeTaskNum of ${viewModel.totalTasksCount}",
                    color = Color.White,
                    fontSize = 20.sp,
                    fontFamily = FontFamily.SansSerif,
                    fontWeight = FontWeight.Light,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.testTag("active_task_progress_text")
                )
            }
        }
    }
}

@Composable
fun HistoryScreen(
    sessions: List<SessionRecord>,
    onBack: () -> Unit
) {
    val dateSdf = remember { SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .padding(24.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "history",
                color = Color.White,
                fontSize = 28.sp,
                fontFamily = FontFamily.SansSerif,
                fontWeight = FontWeight.Normal
            )

            Text(
                text = "back",
                color = Color.White,
                fontSize = 18.sp,
                fontFamily = FontFamily.SansSerif,
                fontWeight = FontWeight.Light,
                modifier = Modifier
                    .testTag("history_back_button")
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) {
                        onBack()
                    }
                    .padding(8.dp)
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        if (sessions.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "no sessions completed yet",
                    color = Color.White,
                    fontSize = 18.sp,
                    fontFamily = FontFamily.SansSerif,
                    fontWeight = FontWeight.Light,
                    textAlign = TextAlign.Center
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                items(sessions) { session ->
                    val dateFormatted = try {
                        dateSdf.format(Date(session.startTimeMillis))
                    } catch (e: Exception) {
                        "date unknown"
                    }

                    val totalAllocatedMin = session.totalAllocatedDurationMillis / 60000L
                    val totalAllocatedSec = (session.totalAllocatedDurationMillis % 60000L) / 1000L
                    val allocatedString = String.format(Locale.US, "%02d:%02d", totalAllocatedMin, totalAllocatedSec)

                    val actualMin = session.totalActualDurationMillis / 60000L
                    val actualSec = (session.totalActualDurationMillis % 60000L) / 1000L
                    val actualString = String.format(Locale.US, "%02d:%02d", actualMin, actualSec)

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("history_session_item")
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = dateFormatted,
                                color = Color.White,
                                fontSize = 16.sp,
                                fontFamily = FontFamily.SansSerif,
                                fontWeight = FontWeight.Normal
                            )
                            Text(
                                text = "target: $allocatedString",
                                color = Color.White,
                                fontSize = 16.sp,
                                fontFamily = FontFamily.SansSerif,
                                fontWeight = FontWeight.Normal
                            )
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "actual: $actualString",
                                color = Color.White,
                                fontSize = 15.sp,
                                fontFamily = FontFamily.SansSerif,
                                fontWeight = FontWeight.Light
                            )
                            Text(
                                text = "tasks: ${session.targetTasksCount}",
                                color = Color.White,
                                fontSize = 15.sp,
                                fontFamily = FontFamily.SansSerif,
                                fontWeight = FontWeight.Light
                            )
                        }

                        // Display splits of each completed task
                        val splits = session.getTaskDurationsList()
                        if (splits.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(6.dp))
                            val splitsBuilder = StringBuilder()
                            splits.forEachIndexed { idx, rawDuration ->
                                val duration = rawDuration.coerceAtLeast(0L)
                                val sec = duration / 1000L
                                val msPercent = (duration % 1000L) / 10L
                                splitsBuilder.append("${idx + 1}: ${sec}.${String.format(Locale.US, "%02d", msPercent)}s")
                                if (idx < splits.size - 1) {
                                    splitsBuilder.append("  |  ")
                                }
                            }
                            Text(
                                text = splitsBuilder.toString(),
                                color = Color.White,
                                fontSize = 13.sp,
                                fontFamily = FontFamily.SansSerif,
                                fontWeight = FontWeight.Light,
                                modifier = Modifier.padding(top = 2.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Custom 1dp thin white separation line between history list items
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(1.dp)
                                .background(Color.White)
                        )
                    }
                }
            }
        }
    }
}
