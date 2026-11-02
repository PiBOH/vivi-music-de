package com.music.vivi.desktop

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * The measured frame rate, in a small pill in the window's top-end corner
 * (Developer options -> Unlock frame rate -> *Show frame rate*).
 *
 * The number is frames actually produced by the window's frame clock in the last
 * second - the same reading the developer panel shows - and the loop lives with
 * the pill, so nothing asks for frames while the pill is not on screen. Every
 * frame it counts is one the window really painted, which is what tells an
 * unlocked 120 Hz panel from a 60 Hz one.
 */
@Composable
fun FpsCounterOverlay(modifier: Modifier = Modifier) {
    var fps by remember { mutableStateOf(0) }
    LaunchedEffect(Unit) {
        var frames = 0
        var windowStart = 0L
        while (true) {
            withFrameNanos { now ->
                if (windowStart == 0L) windowStart = now
                frames++
                val elapsed = now - windowStart
                if (elapsed >= 1_000_000_000L) {
                    fps = (frames * 1_000_000_000L / elapsed).toInt()
                    frames = 0
                    windowStart = now
                }
            }
        }
    }
    Box(modifier) {
        Surface(
            shape = MaterialTheme.shapes.extraSmall,
            color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.92f),
            tonalElevation = 3.dp,
            modifier = Modifier.padding(10.dp),
        ) {
            Text(
                "$fps FPS",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            )
        }
    }
}
