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
import android.util.Log
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
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PlayArrow
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
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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

    // Interface for pattern providers
    interface VibrationPatternProvider {
        fun getPattern(): List<VibrationStep>
        fun getKey(): String
    }

    data class VibrationStep(
        val duration: Long,
        val amplitude: Int,
        val pauseDuration: Long = 100L
    )

    // Static pattern provider
    class StaticPatternProvider(
        private val key: String,
        private val pattern: List<VibrationStep>
    ) : VibrationPatternProvider {
        override fun getPattern() = pattern
        override fun getKey() = key
    }

    // Random pattern provider example
    class RandomPatternProvider(
        private val length: Int,
        private val name: String
    ) : VibrationPatternProvider {
        private var currentPattern: List<VibrationStep>? = null

        override fun getPattern(): List<VibrationStep> {
            if (currentPattern == null) {
                currentPattern = List(length) {
                    val duration = (100L..500L).random()
                    val strength = (30..250).random()
                    val pauseDuration = (20L..500L).random()
                    VibrationStep(duration, strength, pauseDuration)
                }
            }
            return currentPattern!!
        }

        fun generateNewPattern() {
            currentPattern = null
        }

        override fun getKey() = name
    }

    // Iterator for vibration patterns
    class VibrationPatternIterator {
        private val patternProviders = mutableListOf<VibrationPatternProvider>()
        var currentIndex = 0
            private set
        private val randomShortProvider = RandomPatternProvider(12, "Random Short")
        private val randomNormalProvider = RandomPatternProvider(25, "Random Normal")
        private val randomLongProvider = RandomPatternProvider(50, "Random Long")

        init {
            // Add random patterns first
            patternProviders.add(randomShortProvider)
            patternProviders.add(randomNormalProvider)
            patternProviders.add(randomLongProvider)
            
            // Add static patterns
            patternProviders.add(StaticPatternProvider("One", listOf(
                VibrationStep(100L, 50, 100L), VibrationStep(200L, 120, 200L), VibrationStep(300L, 200, 300L),
                VibrationStep(50L, 255, 50L), VibrationStep(400L, 80, 400L), VibrationStep(150L, 180, 150L),
                VibrationStep(250L, 100, 250L), VibrationStep(500L, 150, 500L), VibrationStep(75L, 220, 75L),
                VibrationStep(350L, 70, 350L)
            )))
            patternProviders.add(StaticPatternProvider("Morse", listOf(
                VibrationStep(200L, 255, 200L), VibrationStep(100L, 150, 100L), VibrationStep(200L, 255, 200L),
                VibrationStep(100L, 150, 100L), VibrationStep(50L, 100, 50L), VibrationStep(50L, 100, 50L),
                VibrationStep(50L, 100, 50L), VibrationStep(200L, 255, 200L), VibrationStep(100L, 150, 100L),
                VibrationStep(200L, 255, 200L)
            )))
            patternProviders.add(StaticPatternProvider("Twinkle", listOf(
                VibrationStep(200L, 200, 200L), VibrationStep(100L, 150, 100L), VibrationStep(200L, 200, 200L),
                VibrationStep(100L, 150, 100L), VibrationStep(200L, 180, 200L), VibrationStep(100L, 130, 100L),
                VibrationStep(400L, 255, 400L)
            )))
            patternProviders.add(StaticPatternProvider("Dogs", listOf(
                VibrationStep(200L, 255, 200L), VibrationStep(150L, 200, 150L), VibrationStep(150L, 200, 150L),
                VibrationStep(400L, 255, 400L), VibrationStep(400L, 200, 400L), VibrationStep(150L, 255, 150L),
                VibrationStep(150L, 255, 150L), VibrationStep(150L, 255, 150L), VibrationStep(150L, 255, 150L)
            )))
            patternProviders.add(StaticPatternProvider("Chaos", listOf(
                VibrationStep(50L, 255, 50L), VibrationStep(300L, 80, 300L), VibrationStep(25L, 200, 25L),
                VibrationStep(400L, 255, 400L), VibrationStep(75L, 130, 75L), VibrationStep(200L, 180, 200L),
                VibrationStep(150L, 255, 150L), VibrationStep(350L, 100, 350L), VibrationStep(100L, 220, 100L),
                VibrationStep(250L, 160, 250L), VibrationStep(125L, 90, 125L), VibrationStep(450L, 240, 450L)
            )))
            // Add random pattern providers
        }

        fun getCurrentPattern(): List<VibrationStep> {
            return patternProviders[currentIndex].getPattern()
        }

        fun getCurrentKey(): String {
            return patternProviders[currentIndex].getKey()
        }

        fun next() {
            val currentProvider = patternProviders[currentIndex]
            currentIndex = (currentIndex + 1) % patternProviders.size
            
            // Generate new pattern if switching from any random pattern
            when (currentProvider) {
                is RandomPatternProvider -> currentProvider.generateNewPattern()
            }
        }

        fun previous() {
            val currentProvider = patternProviders[currentIndex]
            currentIndex = if (currentIndex > 0) currentIndex - 1 else patternProviders.size - 1
            
            // Generate new pattern if switching from any random pattern
            when (currentProvider) {
                is RandomPatternProvider -> currentProvider.generateNewPattern()
            }
        }

        fun size(): Int = patternProviders.size
    }

    // SpinnerPatternIterator class to manage spinner patterns
    class SpinnerPatternIterator {
        private val patterns = listOf(
            listOf("⣾", "⣽", "⣻", "⢿", "⡿", "⣟", "⣯", "⣷"),
            listOf("⣷", "⣯", "⣟", "⡿", "⢿", "⣻", "⣽", "⣾"),
            listOf("⠋", "⠙", "⠚", "⠞", "⠖", "⠦", "⠴", "⠲", "⠳", "⠓"),
            listOf("⠋", "⠙", "⠚", "⠒", "⠂", "⠂", "⠒", "⠲", "⠴", "⠦", "⠖", "⠒", "⠐", "⠐", "⠒", "⠓", "⠋"),
            listOf("⠁", "⠉", "⠙", "⠚", "⠒", "⠂", "⠂", "⠒", "⠲", "⠴", "⠤", "⠄", "⠄", "⠤", "⠴", "⠲", "⠒", "⠂", "⠂", "⠒", "⠚", "⠙", "⠉", "⠁"),
            listOf(
                "⢀⠀", "⡀⠀", "⠄⠀", "⢂⠀", "⡂⠀", "⠅⠀", "⢃⠀", "⡃⠀", "⠍⠀", "⢋⠀", "⡋⠀", "⠍⠁", "⢋⠁", 
                "⡋⠁", "⠍⠉", "⠋⠉", "⠋⠉", "⠉⠙", "⠉⠙", "⠉⠩", "⠈⢙", "⠈⡙", "⢈⠩", "⡀⢙", "⠄⡙", "⢂⠩", 
                "⡂⢘", "⠅⡘", "⢃⠨", "⡃⢐", "⠍⡐", "⢋⠠", "⡋⢀", "⠍⡁", "⢋⠁", "⡋⠁", "⠍⠉", "⠋⠉", "⠋⠉", 
                "⠉⠙", "⠉⠙", "⠉⠩", "⠈⢙", "⠈⡙", "⠈⠩", "⠀⢙", "⠀⡙", "⠀⠩", "⠀⢘", "⠀⡘", "⠀⠨", "⠀⢐", 
                "⠀⡐", "⠀⠠", "⠀⢀", "⠀⡀"
            ),
        )
        var currentPatternIndex = 0
            private set
        var currentFrameIndex = 0
            private set
        private var lastPatternIndex = 0

        fun getCurrentFrame(): String {
            return patterns[currentPatternIndex][currentFrameIndex]
        }

        fun nextFrame() {
            currentFrameIndex = (currentFrameIndex + 1) % patterns[currentPatternIndex].size
        }

        fun switchPattern() {
            lastPatternIndex = currentPatternIndex
            val availablePatterns = (0 until patterns.size).filter { it != lastPatternIndex }
            currentPatternIndex = availablePatterns.random()
            currentFrameIndex = 0
        }

        fun isValidPattern(): Boolean {
            return currentPatternIndex in patterns.indices
        }
    }

    // Function to provide vibration feedback for pattern switch
    private suspend fun providePatternSwitchFeedback() {
        repeat(3) {
            vibrator.vibrate(VibrationEffect.createOneShot(200L, 128))
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

        // Check if vibrator is available and working
        if (vibrator.hasVibrator()) {
            // Test vibration to ensure it's working
            vibrator.vibrate(VibrationEffect.createOneShot(200L, VibrationEffect.DEFAULT_AMPLITUDE))
        } else {
            Log.e("MainActivity", "No vibrator available on this device")
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
                            currentPatternIndex = (currentPatternIndex + 1) % VibrationPatternIterator().size()
                            providePatternSwitchFeedback()
                        } else {
                            // Flick in (left)
                            currentPatternIndex = if (currentPatternIndex > 0) currentPatternIndex - 1 else VibrationPatternIterator().size() - 1
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
            WearAppTheme {
                AppScaffold(
                    timeText = { /* Remove time text */ }
                ) {
                    var currentPatternIndex by remember { mutableStateOf(0) }
                    ListScreen(
                        vibrator = vibrator,
                        currentPatternIndex = currentPatternIndex,
                        onPatternChange = { newIndex ->
                            currentPatternIndex = newIndex
                        },
                        onKeepScreenOn = { keepOn ->
                            if (keepOn) {
                                window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                            } else {
                                window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                            }
                        }
                    )
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
        AppScaffold(
            timeText = { /* Remove time text */ }
        ) {
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
    var intensityMultiplier by remember { mutableStateOf(0.5f) }
    var isIntensityVisible by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()
    var vibrationJob by remember { mutableStateOf<Job?>(null) }
    val spinnerIterator = remember { MainActivity.SpinnerPatternIterator() }
    val vibrationIterator = remember { MainActivity.VibrationPatternIterator() }
    var spinnerFrame by remember { mutableStateOf("") }

    // Effect to sync vibration iterator with pattern index
    LaunchedEffect(currentPatternIndex) {
        while (vibrationIterator.currentIndex != currentPatternIndex) {
            if (currentPatternIndex > vibrationIterator.currentIndex) {
                vibrationIterator.next()
            } else {
                vibrationIterator.previous()
            }
        }
    }

    // Spinner animation - always running
    LaunchedEffect(Unit) {
        while (true) {
            if (spinnerIterator.isValidPattern()) {
                spinnerIterator.nextFrame()
                spinnerFrame = spinnerIterator.getCurrentFrame()
            }
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

    Scaffold(
        positionIndicator = { PositionIndicator(scalingLazyListState = rememberScalingLazyListState()) },
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    detectVerticalDragGestures { _, dragAmount ->
                        val delta = if (dragAmount < 0) 0.05f else -0.05f
                        intensityMultiplier = (intensityMultiplier + delta).coerceIn(0f, 1f)
                        coroutineScope.launch {
                            isIntensityVisible = true
                            delay(1000) // Show for 800ms
                            isIntensityVisible = false
                        }
                    }
                }
        ) {
            ScalingLazyColumn(
                modifier = Modifier.fillMaxSize(),
                state = rememberScalingLazyListState(),
                userScrollEnabled = false,
            ) {
                item {
                    Text(
                        text = spinnerFrame,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        textAlign = TextAlign.Center,
                        style = TextStyle(
                            fontSize = 50.sp,
                            color = MaterialTheme.colors.secondary
                        )
                    )
                }
                item {
                    Text(
                        text = "${(intensityMultiplier * 100).toInt()}%",
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Center,
                        style = TextStyle(
                            fontSize = 20.sp,
                            color = Color(0xFFd65d0e)
                        ),
                        color = if (isIntensityVisible) Color(0xFFd65d0e) else Color.Transparent
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
                                        val currentPattern = vibrationIterator.getCurrentPattern()

                                        while (isVibrating) {
                                            for (step in currentPattern) {
                                                if (!isVibrating) break
                                                // Apply intensity multiplier to amplitude
                                                val adjustedAmplitude =
                                                    (step.amplitude * intensityMultiplier)
                                                        .toInt()
                                                        .coerceIn(1, 255)
                                                isMotorActive = true
                                                vibrator.vibrate(
                                                    VibrationEffect.createOneShot(
                                                        step.duration,
                                                        adjustedAmplitude,
                                                    ),
                                                )
                                                isMotorActive = false
                                                delay(step.pauseDuration) // Add the configured gap between vibrations
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
                                imageVector = if (isVibrating) Icons.Filled.Close else Icons.Filled.PlayArrow,
                                contentDescription = if (isVibrating) "Stop Vibration" else "Start Vibration",
                            )
                        }

                        Button(
                            onClick = {
                                coroutineScope.launch {
                                    // Change pattern
                                    vibrationIterator.next()
                                    onPatternChange(vibrationIterator.currentIndex)
                                    // Change spinner type
                                    spinnerIterator.switchPattern()
                                    // Provide feedback
                                    repeat(3) {
                                        vibrator.vibrate(VibrationEffect.createOneShot(200L, 128))
                                    }
                                    // If vibrating, restart with new pattern
                                    if (isVibrating) {
                                        vibrationJob?.cancel()
                                        vibrator.cancel()
                                        vibrationJob = coroutineScope.launch {
                                            while (isVibrating) {
                                                val currentPattern = vibrationIterator.getCurrentPattern()
                                                for (step in currentPattern) {
                                                    if (!isVibrating) break
                                                    val adjustedAmplitude =
                                                        (step.amplitude * intensityMultiplier)
                                                            .toInt()
                                                            .coerceIn(1, 255)
                                                    isMotorActive = true
                                                    vibrator.vibrate(
                                                        VibrationEffect.createOneShot(
                                                            step.duration,
                                                            if (adjustedAmplitude < 10) VibrationEffect.DEFAULT_AMPLITUDE else adjustedAmplitude,
                                                        ),
                                                    )
                                                    delay(step.duration)
                                                    isMotorActive = false
                                                    delay(step.pauseDuration) // Add the configured gap between vibrations
                                                }
                                                delay(100) // Pause before repeating the sequence
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
                                imageVector = Icons.Filled.ArrowForward,
                                contentDescription = "Next Pattern",
                            )
                        }
                    }
                }
                item {
                    Text(
                        text = vibrationIterator.getCurrentKey(),
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.caption1,
                    )
                }
            }
        }
    }

    // Function to switch to next pattern
    suspend fun switchToNextPattern() {
        vibrationIterator.next()
        onPatternChange(vibrationIterator.currentIndex)
        spinnerIterator.switchPattern()
        repeat(3) {
            vibrator.vibrate(VibrationEffect.createOneShot(200L, 128))
        }
    }

    // Function to switch to previous pattern
    suspend fun switchToPreviousPattern() {
        vibrationIterator.previous()
        onPatternChange(vibrationIterator.currentIndex)
        spinnerIterator.switchPattern()
        repeat(3) {
            vibrator.vibrate(VibrationEffect.createOneShot(200L, 128))
        }
    }
}
