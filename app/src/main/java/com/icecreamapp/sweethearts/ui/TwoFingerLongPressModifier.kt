package com.icecreamapp.sweethearts.ui

import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerId
import androidx.compose.ui.input.pointer.pointerInput
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.coroutines.coroutineContext

/**
 * Calls [onTrigger] when two fingers are held down for [durationMs] milliseconds.
 * Tracks distinct pointers by ID so one finger (e.g. down + move) is not counted twice.
 */
fun Modifier.twoFingerLongPress(
    durationMs: Long = 2000L,
    onTrigger: () -> Unit,
): Modifier = pointerInput(Unit) {
    val pressedPointerIds = mutableSetOf<PointerId>()
    var holdJob: Job? = null
    val scope = CoroutineScope(coroutineContext)
    awaitPointerEventScope {
        while (true) {
            val event = awaitPointerEvent()
            event.changes.forEach { change ->
                if (change.pressed) {
                    pressedPointerIds.add(change.id)
                } else {
                    pressedPointerIds.remove(change.id)
                }
            }
            holdJob?.cancel()
            if (pressedPointerIds.size >= 2) {
                holdJob = scope.launch {
                    delay(durationMs)
                    onTrigger()
                }
            } else {
                holdJob = null
            }
        }
    }
}

