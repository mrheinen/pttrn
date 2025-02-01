/*
 * Copyright 2022 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
@file:OptIn(ExperimentalHorologistApi::class, ExperimentalWearFoundationApi::class)

package com.lophiid.pttrn.presentation

import android.content.Context
import android.os.Build
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.view.GestureDetector
import android.view.GestureDetector.SimpleOnGestureListener
import android.view.MotionEvent
import android.view.Window
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.wear.compose.foundation.ExperimentalWearFoundationApi
import androidx.wear.compose.foundation.lazy.ScalingLazyColumn
import androidx.wear.compose.foundation.lazy.rememberScalingLazyListState
import androidx.wear.compose.material.Button
import androidx.wear.compose.material.ButtonDefaults
import androidx.wear.compose.material.Icon
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.PositionIndicator
import androidx.wear.compose.material.Scaffold
import androidx.wear.compose.material.Text
import androidx.wear.compose.material.TimeText
import com.google.android.horologist.annotations.ExperimentalHorologistApi
import com.google.android.horologist.compose.layout.AppScaffold
import com.lophiid.pttrn.presentation.theme.WearAppTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.abs

/**
 * Simple "Hello, World" app meant as a starting point for a new project using Compose for Wear OS.
 *
 * Displays a centered [Text] composable and a list built with [Horologist]
 * (https://github.com/google/horologist).
 *
 * Use the Wear version of Compose Navigation. You can carry
 * over your knowledge from mobile and it supports the swipe-to-dismiss gesture (Wear OS's
 * back action). For more information, go here:
 * https://developer.android.com/reference/kotlin/androidx/wear/compose/navigation/package-summary
 */
class MainActivity : ComponentActivity() {
    private lateinit var vibrator: Vibrator
    private lateinit var gestureDetector: GestureDetector
    private var currentPatternIndex by mutableStateOf(0)
    private var coroutineScope = CoroutineScope(Dispatchers.Main + Job())

    // Define vibration patterns with names
    private val vibrationPatterns = listOf(
        "One" to listOf( // Pattern One - Rhythmic
            Pair(100L, 50), // Quick, light tap
            Pair(200L, 120), // Medium, moderate intensity
            Pair(300L, 200), // Long, strong vibration
            Pair(50L, 255), // Very short, maximum intensity
            Pair(400L, 80), // Extended, gentle vibration
            Pair(150L, 180), // Short, strong pulse
            Pair(250L, 100), // Medium, moderate pulse
            Pair(500L, 150), // Very long, medium-strong
            Pair(75L, 220), // Quick, very strong
            Pair(350L, 70), // Long, gentle
        ),
        "Morse" to listOf( // Pattern Two - Morse Code Style
            Pair(200L, 255), // Long strong
            Pair(100L, 150), // Short medium
            Pair(200L, 255), // Long strong
            Pair(100L, 150), // Short medium
            Pair(50L, 100), // Quick light
            Pair(50L, 100), // Quick light
            Pair(50L, 100), // Quick light
            Pair(200L, 255), // Long strong
            Pair(100L, 150), // Short medium
            Pair(200L, 255), // Long strong
        ),
        "Twinkle" to listOf( // Pattern Three - Twinkle Twinkle Little Star
            Pair(200L, 200), // Twin-
            Pair(100L, 150), // -kle
            Pair(200L, 200), // twin-
            Pair(100L, 150), // -kle
            Pair(200L, 180), // lit-
            Pair(100L, 130), // -tle
            Pair(400L, 255), // star
        ),
        "Dogs" to listOf( // Pattern Four - Who Let The Dogs Out
            Pair(200L, 255), // WHO
            Pair(150L, 200), // LET
            Pair(150L, 200), // THE
            Pair(400L, 255), // DOGS
            Pair(400L, 200), // OUT
            Pair(150L, 255), // WOOF
            Pair(150L, 255), // WOOF
            Pair(150L, 255), // WOOF
            Pair(150L, 255), // WOOF
        ),
        "Chaos" to listOf( // Pattern Five - Random Chaos
            Pair(50L, 255), // Sudden strong zap
            Pair(300L, 80), // Long gentle hum
            Pair(25L, 200), // Tiny medium pulse
            Pair(400L, 255), // Extended strong buzz
            Pair(75L, 130), // Quick light tap
            Pair(200L, 180), // Medium balanced pulse
            Pair(150L, 255), // Short strong burst
            Pair(350L, 100), // Long soft wave
            Pair(100L, 220), // Medium strong pop
            Pair(250L, 160), // Moderate balanced pulse
            Pair(125L, 90), // Short gentle touch
            Pair(450L, 240), // Final strong fade
        ),
    )

    // Function to provide vibration feedback for pattern switch
    private suspend fun providePatternSwitchFeedback() {
        repeat(3) {
            vibrator.vibrate(VibrationEffect.createOneShot(200L, 128))
            delay(250) // Wait between vibrations
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager = getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
            vibratorManager.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }

        // Initialize the gesture detector
        gestureDetector = GestureDetector(this, object : SimpleOnGestureListener() {
            override fun onFling(
                e1: MotionEvent?,
                e2: MotionEvent,
                velocityX: Float,
                velocityY: Float
            ): Boolean {
                // Check if it's a flick gesture based on velocity
                val FLICK_VELOCITY_THRESHOLD = 1000f
                
                if (abs(velocityX) > FLICK_VELOCITY_THRESHOLD) {
                    coroutineScope.launch {
                        if (velocityX > 0) {
                            // Flick out (right)
                            currentPatternIndex = (currentPatternIndex + 1) % vibrationPatterns.size
                            providePatternSwitchFeedback()
                        } else {
                            // Flick in (left)
                            currentPatternIndex = if (currentPatternIndex > 0) currentPatternIndex - 1 else vibrationPatterns.size - 1
                            providePatternSwitchFeedback()
                        }
                    }
                    return true
                }
                return false
            }
        })

        // Override the window callback to intercept touch events
        val originalCallback = window.callback
        window.callback = object : Window.Callback by originalCallback {
            override fun dispatchTouchEvent(event: MotionEvent): Boolean {
                // Let the gesture detector try to handle it first
                val consumed = gestureDetector.onTouchEvent(event)
                // If the gesture detector didn't consume it, pass it to the original callback
                return if (!consumed) {
                    originalCallback.dispatchTouchEvent(event)
                } else {
                    true
                }
            }
        }

        setContent {
            WearApp(
                vibrator = vibrator,
                currentPatternIndex = currentPatternIndex,
                onPatternChange = { newIndex -> 
                    currentPatternIndex = newIndex
                }
            ) { keepScreenOn ->
                if (keepScreenOn) {
                    window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                } else {
                    window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        coroutineScope.cancel()
    }
}

@Composable
fun WearApp(
    vibrator: Vibrator,
    currentPatternIndex: Int,
    onPatternChange: (Int) -> Unit,
    onKeepScreenOn: (Boolean) -> Unit
) {
    WearAppTheme {
        AppScaffold {
            ListScreen(
                vibrator = vibrator,
                currentPatternIndex = currentPatternIndex,
                onPatternChange = onPatternChange,
                onKeepScreenOn = onKeepScreenOn
            )
        }
    }
}

@Composable
fun ListScreen(
    vibrator: Vibrator,
    currentPatternIndex: Int,
    onPatternChange: (Int) -> Unit,
    onKeepScreenOn: (Boolean) -> Unit
) {
    var isVibrating by remember { mutableStateOf(false) }
    var isMotorActive by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()
    var vibrationJob by remember { mutableStateOf<Job?>(null) }
    var spinnerIndex by remember { mutableStateOf(0) }
    var currentSpinnerType by remember { mutableStateOf(0) }
    var lastSpinnerType by remember { mutableStateOf(-1) }
    var intensityMultiplier by remember { mutableStateOf(0.5f) }

    val rotaryScrollState = rememberScalingLazyListState()

    val spinnerPatterns = listOf(
        listOf("⣾", "⣽", "⣻", "⢿", "⡿", "⣟", "⣯", "⣷"),
        listOf("⣷", "⣯", "⣟", "⡿", "⢿", "⣻", "⣽", "⣾"),
        listOf("⠋", "⠙", "⠚", "⠞", "⠖", "⠦", "⠴", "⠲", "⠳", "⠓"),
        listOf(
            "( ●    )", "(  ●   )", "(   ●  )", "(    ● )", "(     ●)",
            "(    ● )", "(   ●  )", "(  ●   )", "( ●    )", "(●     )",
        ),
        listOf("▸▹▹▹▹", "▹▸▹▹▹", "▹▹▸▹▹", "▹▹▹▸▹", "▹▹▹▹▸"),
        listOf("_", "_", "_", "-", "`", "`", "'", "´", "-", "_", "_", "_"),
    )

    // Function to get next random spinner type different from last
    fun getNextSpinnerType(): Int {
        val availableTypes = (0 until spinnerPatterns.size).filter { it != lastSpinnerType }
        spinnerIndex = 0 // Reset spinner index when changing pattern
        return availableTypes.random()
    }

    // Spinner animation - always running
    LaunchedEffect(Unit) {
        while (true) {
            spinnerIndex = (spinnerIndex + 1) % spinnerPatterns[currentSpinnerType].size
            delay(150) // Control spinning speed
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            vibrationJob?.cancel()
            vibrator.cancel()
            onKeepScreenOn(false)
        }
    }

    // Define vibration patterns with names
    val vibrationPatterns = listOf(
        "One" to listOf( // Pattern One - Rhythmic
            Pair(100L, 50), // Quick, light tap
            Pair(200L, 120), // Medium, moderate intensity
            Pair(300L, 200), // Long, strong vibration
            Pair(50L, 255), // Very short, maximum intensity
            Pair(400L, 80), // Extended, gentle vibration
            Pair(150L, 180), // Short, strong pulse
            Pair(250L, 100), // Medium, moderate pulse
            Pair(500L, 150), // Very long, medium-strong
            Pair(75L, 220), // Quick, very strong
            Pair(350L, 70), // Long, gentle
        ),
        "Morse" to listOf( // Pattern Two - Morse Code Style
            Pair(200L, 255), // Long strong
            Pair(100L, 150), // Short medium
            Pair(200L, 255), // Long strong
            Pair(100L, 150), // Short medium
            Pair(50L, 100), // Quick light
            Pair(50L, 100), // Quick light
            Pair(50L, 100), // Quick light
            Pair(200L, 255), // Long strong
            Pair(100L, 150), // Short medium
            Pair(200L, 255), // Long strong
        ),
        "Twinkle" to listOf( // Pattern Three - Twinkle Twinkle Little Star
            Pair(200L, 200), // Twin-
            Pair(100L, 150), // -kle
            Pair(200L, 200), // twin-
            Pair(100L, 150), // -kle
            Pair(200L, 180), // lit-
            Pair(100L, 130), // -tle
            Pair(400L, 255), // star
        ),
        "Dogs" to listOf( // Pattern Four - Who Let The Dogs Out
            Pair(200L, 255), // WHO
            Pair(150L, 200), // LET
            Pair(150L, 200), // THE
            Pair(400L, 255), // DOGS
            Pair(400L, 200), // OUT
            Pair(150L, 255), // WOOF
            Pair(150L, 255), // WOOF
            Pair(150L, 255), // WOOF
            Pair(150L, 255), // WOOF
        ),
        "Chaos" to listOf( // Pattern Five - Random Chaos
            Pair(50L, 255), // Sudden strong zap
            Pair(300L, 80), // Long gentle hum
            Pair(25L, 200), // Tiny medium pulse
            Pair(400L, 255), // Extended strong buzz
            Pair(75L, 130), // Quick light tap
            Pair(200L, 180), // Medium balanced pulse
            Pair(150L, 255), // Short strong burst
            Pair(350L, 100), // Long soft wave
            Pair(100L, 220), // Medium strong pop
            Pair(250L, 160), // Moderate balanced pulse
            Pair(125L, 90), // Short gentle touch
            Pair(450L, 240), // Final strong fade
        ),
    )

    // Function to provide vibration feedback for pattern switch
    suspend fun providePatternSwitchFeedback() {
        repeat(3) {
            vibrator.vibrate(VibrationEffect.createOneShot(200L, 128))
            delay(250) // Wait between vibrations
        }
    }

    // Function to switch to next pattern
    suspend fun switchToNextPattern() {
        onPatternChange((currentPatternIndex + 1) % vibrationPatterns.size)
        lastSpinnerType = currentSpinnerType
        currentSpinnerType = getNextSpinnerType()
        providePatternSwitchFeedback()
    }

    // Function to switch to previous pattern
    suspend fun switchToPreviousPattern() {
        onPatternChange(if (currentPatternIndex > 0) currentPatternIndex - 1 else vibrationPatterns.size - 1)
        lastSpinnerType = currentSpinnerType
        currentSpinnerType = getNextSpinnerType()
        providePatternSwitchFeedback()
    }

    Scaffold(
        timeText = { TimeText() },
        positionIndicator = { PositionIndicator(scalingLazyListState = rotaryScrollState) },
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    detectVerticalDragGestures { change, dragAmount ->
                        change.consume()
                        // Negative dragAmount means swipe up, positive means swipe down
                        // Scale the drag amount to make it less sensitive
                        val delta = if (dragAmount < 0) 0.05f else -0.05f
                        intensityMultiplier = (intensityMultiplier + delta).coerceIn(0f, 1f)
                    }
                }
        ) {
            ScalingLazyColumn(
                modifier = Modifier.fillMaxSize(),
                state = rotaryScrollState,
                userScrollEnabled = false,
            ) {
                item {
                    Text(
                        text = spinnerPatterns[currentSpinnerType][spinnerIndex],
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.title1,
                        color = if (isMotorActive) Color(0xFF98971a) else Color(0xFF98971a).copy(alpha = 0.6f),
                    )
                }
                item {
                    Text(
                        text = "Intensity: ${(intensityMultiplier * 100).toInt()}%",
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.caption2,
                    )
                }
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                    ) {
                        Button(
                            onClick = {
                                if (!isVibrating) {
                                    onKeepScreenOn(true)
                                    vibrationJob = coroutineScope.launch {
                                        isVibrating = true
                                        while (isVibrating) {
                                            // Play vibration pattern
                                            for ((duration, amplitude) in vibrationPatterns[currentPatternIndex].second) {
                                                if (!isVibrating) break
                                                // Apply intensity multiplier to amplitude
                                                val adjustedAmplitude =
                                                    (amplitude * intensityMultiplier)
                                                        .toInt()
                                                        .coerceIn(1, 255)
                                                isMotorActive = true
                                                vibrator.vibrate(
                                                    VibrationEffect.createOneShot(
                                                        duration,
                                                        adjustedAmplitude,
                                                    ),
                                                )
                                                delay(duration) // Wait for the vibration to complete
                                                isMotorActive = false
                                                delay(100) // Add a small gap between vibrations
                                            }
                                            delay(500) // Pause before repeating the sequence
                                        }
                                    }
                                } else {
                                    vibrationJob?.cancel()
                                    vibrator.cancel()
                                    isVibrating = false
                                    onKeepScreenOn(false)
                                }
                            },
                            modifier = Modifier.size(50.dp),
                            colors = ButtonDefaults.buttonColors(
                                backgroundColor = androidx.compose.ui.graphics.Color(0xFFb57614),
                                contentColor = androidx.compose.ui.graphics.Color.White,
                            ),
                        ) {
                            Icon(
                                imageVector = if (isVibrating) Icons.Default.Close else Icons.Default.PlayArrow,
                                contentDescription = if (isVibrating) "Stop Vibration" else "Start Vibration",
                            )
                        }

                        Button(
                            onClick = {
                                coroutineScope.launch {
                                    // Change pattern
                                    onPatternChange((currentPatternIndex + 1) % vibrationPatterns.size)
                                    // Change spinner type
                                    lastSpinnerType = currentSpinnerType
                                    currentSpinnerType = getNextSpinnerType()
                                    // Provide feedback
                                    repeat(3) {
                                        vibrator.vibrate(VibrationEffect.createOneShot(200L, 128))
                                        delay(250) // Wait between vibrations
                                    }
                                    // If vibrating, restart with new pattern
                                    if (isVibrating) {
                                        vibrationJob?.cancel()
                                        vibrator.cancel()
                                        vibrationJob = coroutineScope.launch {
                                            while (isVibrating) {
                                                for ((duration, amplitude) in vibrationPatterns[currentPatternIndex].second) {
                                                    if (!isVibrating) break
                                                    val adjustedAmplitude =
                                                        (amplitude * intensityMultiplier)
                                                            .toInt()
                                                            .coerceIn(1, 255)
                                                    isMotorActive = true
                                                    vibrator.vibrate(
                                                        VibrationEffect.createOneShot(
                                                            duration,
                                                            adjustedAmplitude,
                                                        ),
                                                    )
                                                    delay(duration)
                                                    isMotorActive = false
                                                    delay(100)
                                                }
                                                delay(500)
                                            }
                                        }
                                    }
                                }
                            },
                            modifier = Modifier.size(50.dp),
                            colors = ButtonDefaults.buttonColors(
                                backgroundColor = androidx.compose.ui.graphics.Color(0xFFb57614),
                                contentColor = androidx.compose.ui.graphics.Color.White,
                            ),
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                contentDescription = "Next Pattern",
                            )
                        }
                    }
                }
                item {
                    Text(
                        text = vibrationPatterns[currentPatternIndex].first,
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.caption1,
                    )
                }
            }
        }
    }
}
