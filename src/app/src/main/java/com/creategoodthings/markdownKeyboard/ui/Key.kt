package com.creategoodthings.markdownKeyboard.ui

import android.content.Context.AUDIO_SERVICE
import android.media.AudioManager
import android.view.HapticFeedbackConstants
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.takeOrElse
import com.creategoodthings.markdownKeyboard.editor.KeyAction
import com.creategoodthings.markdownKeyboard.ui.theme.LocalKeyboardColors
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

private const val REPEAT_TIMEOUT_MS = 60_000L
private const val REPEAT_INTERVAL_MS = 60L
private const val KEY_CORNER_RADIUS = 7f
private const val KEY_MIN_HEIGHT = 40f
private const val KEY_FONT_SIZE = 22f
/**
 * Room reserved around a label inside its key. Deliberately small: the keys are weighted, so
 * anything spent here is width the row cannot give back, and the space goes to the keyboard's
 * outer edges instead (`KEYBOARD_EDGE_PADDING`).
 */
private const val KEY_HORIZONTAL_PADDING = 4f
private const val VIBRATE_ON_CLICK = true
private const val SOUND_ON_CLICK = false

@OptIn(ExperimentalFoundationApi::class)
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
    var repeating by remember { mutableStateOf(false) }

    val colors = LocalKeyboardColors.current.forTone(key.tone, isPressed)

    fun feedback() {
        if (VIBRATE_ON_CLICK) view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
        if (SOUND_ON_CLICK) audioManager.playSoundEffect(AudioManager.FX_KEY_CLICK, 0.1f)
    }

    fun startRepeating() {
        repeating = true
        // Stays on the main dispatcher: InputConnection is not safe to touch off it.
        scope.launch {
            withTimeoutOrNull(REPEAT_TIMEOUT_MS) {
                while (true) {
                    onAction(key.action)
                    delay(REPEAT_INTERVAL_MS)
                }
            }
        }
    }

    LaunchedEffect(isPressed) {
        if (isPressed) {
            feedback()
        } else if (repeating) {
            scope.coroutineContext.cancelChildren()
            repeating = false
        }
    }

    Box(
        modifier = modifier
            .defaultMinSize(minHeight = KEY_MIN_HEIGHT.dp)
            .clip(RoundedCornerShape(KEY_CORNER_RADIUS.dp))
            .background(colors.background)
            .combinedClickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = {
                    onAction(key.action)
                    onClick?.invoke()
                },
                onLongClick = {
                    feedback()
                    val longPress = key.longPressAction
                    when {
                        longPress != null -> onAction(longPress)
                        key.repeatable -> startRepeating()
                        else -> onAction(key.action)
                    }
                },
            ),
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
