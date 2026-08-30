package com.creategoodthings.markdownKeyboard.ui

import android.content.Context.AUDIO_SERVICE
import android.media.AudioManager
import android.view.HapticFeedbackConstants
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.interaction.Interaction
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.AwaitPointerEventScope
import androidx.compose.ui.input.pointer.PointerEventTimeoutCancellationException
import androidx.compose.ui.input.pointer.PointerId
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.onLongClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.takeOrElse
import androidx.compose.ui.util.fastAll
import androidx.compose.ui.util.fastAny
import androidx.compose.ui.util.fastFirstOrNull
import androidx.compose.ui.util.fastForEach
import com.creategoodthings.markdownKeyboard.editor.KeyAction
import com.creategoodthings.markdownKeyboard.ui.theme.LocalKeyboardColors
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.withTimeoutOrNull

private const val REPEAT_TIMEOUT_MS = 60_000L
private const val REPEAT_INTERVAL_MS = 60L
private const val KEY_CORNER_RADIUS = 7f
internal const val KEY_HEIGHT = 40f
internal const val KEY_FONT_SIZE = 22f
/**
 * Room reserved around a label inside its key. Deliberately small: the keys are weighted, so
 * anything spent here is width the row cannot give back, and the space goes to the keyboard's
 * outer edges instead (`KEYBOARD_EDGE_PADDING`).
 */
private const val KEY_HORIZONTAL_PADDING = 4f
private const val VIBRATE_ON_CLICK = true
private const val SOUND_ON_CLICK = false

/**
 * Everything the gesture loop reads that a recomposition can change underneath it.
 *
 * Held behind a [rememberUpdatedState] so `pointerInput` can be keyed on `Unit`: [KeyItem] is
 * rebuilt inline on every recomposition and shift rewrites a letter key's contents outright, so
 * keying the gesture on the key would cancel a press in flight the moment shift changed.
 */
private class KeyBinding(
    val key: KeyItem,
    val onAction: (KeyAction) -> Unit,
    val onClick: (() -> Unit)?,
)

/**
 * The key's place in the keyboard, for positioning an alternates strip over it.
 *
 * Deliberately not a `mutableStateOf`: it is written on every layout pass and read only from the
 * gesture coroutine, so making it observable would invalidate composition for nothing.
 */
private class KeyPosition {
    var coordinates: LayoutCoordinates? = null
}

@Composable
fun Key(
    key: KeyItem,
    onAction: (KeyAction) -> Unit,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val view = LocalView.current
    val audioManager = LocalContext.current.getSystemService(AUDIO_SERVICE) as AudioManager
    val scope = rememberCoroutineScope()
    val host = LocalAlternatesHost.current
    val binding = rememberUpdatedState(KeyBinding(key, onAction, onClick))
    val position = remember { KeyPosition() }

    val colors = LocalKeyboardColors.current.forTone(key.tone, isPressed)

    fun feedback(constant: Int) {
        if (VIBRATE_ON_CLICK) view.performHapticFeedback(constant)
        if (SOUND_ON_CLICK) audioManager.playSoundEffect(AudioManager.FX_KEY_CLICK, 0.1f)
    }

    // Built out here rather than inside the gesture: `AwaitPointerEventScope` restricts
    // suspension, and that restriction reaches into any suspend lambda declared under it.
    // Stays on the main dispatcher either way — InputConnection is not safe off it.
    fun startRepeating(): Job = scope.launch {
        withTimeoutOrNull(REPEAT_TIMEOUT_MS) {
            while (isActive) {
                binding.value.let { it.onAction(it.key.action) }
                delay(REPEAT_INTERVAL_MS)
            }
        }
    }

    Box(
        modifier = modifier
            .defaultMinSize(minHeight = KEY_HEIGHT.dp)
            .clip(RoundedCornerShape(KEY_CORNER_RADIUS.dp))
            .background(colors.background)
            .onGloballyPositioned { position.coordinates = it }
            .keySemantics(binding)
            .pointerInput(Unit) {
                awaitEachGesture {
                    val down = awaitFirstDown()
                    down.consume()
                    val press = PressInteraction.Press(down.position)
                    scope.launch { interactionSource.emit(press) }
                    feedback(HapticFeedbackConstants.KEYBOARD_TAP)

                    var release: Interaction = PressInteraction.Release(press)
                    var repeatJob: Job? = null
                    var stripOpen = false
                    try {
                        var up: PointerInputChange? = null
                        var held = false
                        try {
                            withTimeout(viewConfiguration.longPressTimeoutMillis) {
                                up = waitForUpOrCancellation()
                            }
                        } catch (_: PointerEventTimeoutCancellationException) {
                            held = true
                        }

                        if (!held) {
                            val change = up
                            // A finger that wandered off the key before lifting typed nothing,
                            // which is what `combinedClickable` used to give us for free.
                            if (change == null || !change.position.within(size)) {
                                release = PressInteraction.Cancel(press)
                            } else {
                                change.consume()
                                val bound = binding.value
                                bound.onAction(bound.key.action)
                                bound.onClick?.invoke()
                            }
                            return@awaitEachGesture
                        }

                        feedback(HapticFeedbackConstants.LONG_PRESS)
                        val bound = binding.value
                        when (val hold = bound.key.hold) {
                            HoldBehaviour.None -> {
                                bound.onAction(bound.key.action)
                                consumeUntilUp()
                            }

                            is HoldBehaviour.Action -> {
                                bound.onAction(hold.action)
                                consumeUntilUp()
                            }

                            HoldBehaviour.Repeat -> {
                                repeatJob = startRepeating()
                                consumeUntilUp()
                            }

                            is HoldBehaviour.Alternates -> {
                                stripOpen = host != null &&
                                    host.open(position.coordinates, hold.values, hold.baseIndex)
                                if (!stripOpen) {
                                    // No overlay, or no room across the keyboard: degrade to the
                                    // plain long press rather than swallowing the gesture.
                                    bound.onAction(bound.key.action)
                                    consumeUntilUp()
                                } else {
                                    trackStrip(host!!, down.id) {
                                        feedback(HapticFeedbackConstants.CLOCK_TICK)
                                    }
                                    stripOpen = false
                                    host.close()?.let { picked ->
                                        binding.value.let {
                                            it.onAction(KeyAction.CommitText(picked))
                                            // Spends a one-shot shift, as typing the key would.
                                            it.onClick?.invoke()
                                        }
                                    }
                                }
                            }
                        }
                    } finally {
                        repeatJob?.cancel()
                        if (stripOpen) host?.close()
                        scope.launch { interactionSource.emit(release) }
                    }
                }
            },
    ) {
        val description = key.label.description?.let { stringResource(it) }
        val content = Modifier
            .align(Alignment.Center)
            .padding(horizontal = KEY_HORIZONTAL_PADDING.dp)

        when (val label = key.label) {
            is KeyLabel.Text -> Text(
                text = label.value,
                fontSize = label.fontSize.takeOrElse { KEY_FONT_SIZE.sp },
                color = colors.foreground,
                modifier = content,
            )

            is KeyLabel.Icon -> Icon(
                imageVector = label.image,
                contentDescription = description,
                tint = colors.foreground,
                modifier = content,
            )
        }
    }
}

/**
 * What `combinedClickable` used to supply and a raw `pointerInput` does not.
 *
 * Alternates get listed as custom actions rather than a long press: with TalkBack on, explore by
 * touch means the key never sees the raw pointer stream, so there is no finger to slide.
 */
private fun Modifier.keySemantics(binding: State<KeyBinding>): Modifier =
    semantics(mergeDescendants = true) {
        role = Role.Button
        onClick {
            binding.value.let { it.onAction(it.key.action); it.onClick?.invoke() }
            true
        }
        when (val hold = binding.value.key.hold) {
            HoldBehaviour.None -> Unit

            is HoldBehaviour.Action -> onLongClick {
                binding.value.onAction(hold.action)
                true
            }

            HoldBehaviour.Repeat -> onLongClick {
                binding.value.let { it.onAction(it.key.action) }
                true
            }

            is HoldBehaviour.Alternates -> customActions = hold.values
                .filterIndexed { index, _ -> index != hold.baseIndex }
                .map { value ->
                    CustomAccessibilityAction(value) {
                        binding.value.let {
                            it.onAction(KeyAction.CommitText(value))
                            it.onClick?.invoke()
                        }
                        true
                    }
                }
        }
    }

private fun Offset.within(size: IntSize) =
    x >= 0f && y >= 0f && x < size.width && y < size.height

/** foundation keeps its own `consumeUntilUp` private; these are the same three lines. */
private suspend fun AwaitPointerEventScope.consumeUntilUp() {
    do {
        val event = awaitPointerEvent()
        event.changes.fastForEach { it.consume() }
    } while (event.changes.fastAny { it.pressed })
}

/**
 * Feeds the finger to the open strip until this pointer lifts.
 *
 * Positions stay key-local; the host knows where the key is and does the one addition itself.
 */
private suspend fun AwaitPointerEventScope.trackStrip(
    host: AlternatesHostState,
    pointer: PointerId,
    onHighlightChanged: () -> Unit,
) {
    while (true) {
        val event = awaitPointerEvent()
        val change = event.changes.fastFirstOrNull { it.id == pointer }
        if (change == null) {
            // Our pointer vanished; wait out any others so the gesture ends cleanly.
            if (event.changes.fastAll { !it.pressed }) return
            continue
        }
        if (host.move(change.position)) onHighlightChanged()
        change.consume()
        if (!change.pressed) return
    }
}
