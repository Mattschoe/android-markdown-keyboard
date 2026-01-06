package com.creategoodthings.markdownKeyboard.ui

import android.content.Context.AUDIO_SERVICE
import android.media.AudioManager
import android.view.HapticFeedbackConstants
import android.view.KeyEvent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsBottomHeight
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.creategoodthings.markdownKeyboard.R
import com.creategoodthings.markdownKeyboard.service.MdIMEService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout
import com.creategoodthings.markdownKeyboard.ui.KeyType.*
import com.creategoodthings.markdownKeyboard.ui.KeyAction.*


private val firstRow = listOf(null)
private val secondRow = listOf("q", "w", "e", "r", "t", "y", "u", "i", "o", "p")
private val thirdRow = listOf("a", "s", "d", "f", "g", "h", "j", "k", "l")
private val fourthRow = listOf("z", "x", "c", "v", "b", "n", "m")

private const val SPACE_BETWEEN_ROWS = 10f
private const val SPACE_BETWEEN_KEYS = 2.5f

private const val ITALIC = "*"
private const val BOLD = "**"
private const val CODE = "`"
private const val UNORDERED_LIST = "- "
private const val ORDERED_LIST = "1. "
private const val CHECKBOX = "- [ ] "

@Composable
fun MarkdownKeyboard() {
    var capslock by remember { mutableStateOf(false) }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.background)
    ) {
        //region FIRST ROW
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(SPACE_BETWEEN_KEYS.dp)
        ) {
            //Bold
            Key(
                key = KeyItem(
                    keyAction = BoldSelection,
                    keyType = KeyIcon(ImageVector.vectorResource(R.drawable.bold_icon))
                )
            )

            //Italic
            Key(
                key = KeyItem(
                    keyAction = CursiveSelection,
                    keyType = KeyIcon(ImageVector.vectorResource(R.drawable.italic_icon))
                )
            )

            //Unordered List
            Key(
                KeyItem(
                    keyAction = CommitText(UNORDERED_LIST),
                    keyType = KeyIcon(ImageVector.vectorResource(R.drawable.unordered_list_icon))
                )
            )
            //Ordered List
            Key(
                KeyItem(
                    keyAction = CommitText(ORDERED_LIST),
                    keyType = KeyIcon(ImageVector.vectorResource(R.drawable.ordered_list_icon))
                )
            )

            //Checkbox
            Key(
                KeyItem(
                    keyAction = CommitText(CHECKBOX),
                    keyType = KeyIcon(ImageVector.vectorResource(R.drawable.checkbox_icon))
                )
            )

            //Code
            Key(
                key = KeyItem(
                    keyAction = CodeSelection,
                    keyType = KeyIcon(ImageVector.vectorResource(R.drawable.code_icon))
                )
            )
        }
        //endregion
        Spacer(Modifier.height(SPACE_BETWEEN_ROWS.dp))
        //region SECOND ROW
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(SPACE_BETWEEN_KEYS.dp)
        ) {
            for (key in secondRow) {
                val value = if (capslock) key.uppercase() else key
                Key(
                    key = KeyItem(
                        keyAction = CommitText(text = value),
                        keyType = KeyText(value = value)
                    ),
                    modifier = Modifier.weight(1f)
                )
            }
        }
        //endregion
        Spacer(Modifier.height(SPACE_BETWEEN_ROWS.dp))
        //region THIRD ROW
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(SPACE_BETWEEN_KEYS.dp)
        ) {
            Key(
                KeyItem(
                    keyAction = IndentForward,
                    keyType = KeyIcon(ImageVector.vectorResource(R.drawable.tab_in_icon))
                ),
                modifier = Modifier.weight(1.5f)
            )
            for (key in thirdRow) {
                val value = if (capslock) key.uppercase() else key
                Key(
                    key = KeyItem(
                        keyAction = CommitText(text = value),
                        keyType = KeyText(value = value)
                    ),
                    modifier = Modifier.weight(1f)
                )
            }
            Key(
                KeyItem(
                    keyAction = IndentBack,
                    keyType = KeyIcon(ImageVector.vectorResource(R.drawable.tab_out_icon))
                ),
                modifier = Modifier.weight(1.5f)
            )
        }
        //endregion
        Spacer(Modifier.height(SPACE_BETWEEN_ROWS.dp))
        //region FOURTH ROW
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(SPACE_BETWEEN_KEYS.dp)
        ) {
            Key(
                KeyItem(
                    keyAction = Empty,
                    keyType = KeyIcon(ImageVector.vectorResource(R.drawable.shift_icon))
                ),
                onClick = { capslock = !capslock },
                modifier = Modifier.weight(1.5f)
            )
            for (key in fourthRow) {
                val value = if (capslock) key.uppercase() else key
                Key(
                    key = KeyItem(
                        keyAction = CommitText(text = value),
                        keyType = KeyText(value = value)
                    ),
                    modifier = Modifier.weight(1f)
                )
            }
            Key(
                KeyItem(
                    keyAction = Delete,
                    keyType = KeyIcon(ImageVector.vectorResource(R.drawable.delete_icon))
                ),
                modifier = Modifier.weight(1.5f)
            )
        }
        //endregion
        Spacer(Modifier.height(SPACE_BETWEEN_ROWS.dp))
        //region FIFTH ROW
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(SPACE_BETWEEN_KEYS.dp)
        ) {
            //Numbers + Special characters
            Key(
                KeyItem(
                    keyAction = Empty,
                    keyType = KeyText("?123")
                ),
                onClick = { }
            )

            //Emoji
            Key(
                KeyItem(
                    keyAction = Empty,
                    keyType = KeyIcon(ImageVector.vectorResource(R.drawable.emoji_icon))
                ),
                onClick = { }
            )
            //Comma
            Key(
                KeyItem(
                    keyAction = CommitText(","),
                    keyType = KeyText(",")
                )
            )

            //Space
            Key(
                KeyItem(
                    keyAction = CommitText(" "),
                    keyType = KeyText(" ")
                ),
                modifier = Modifier.weight(1f)
            )

            //Dot
            Key(
                KeyItem(
                    keyAction = CommitText("."),
                    keyType = KeyText(".")
                )
            )

            //Enter
            Key(
                KeyItem(
                    keyAction = Enter,
                    keyType = KeyIcon(ImageVector.vectorResource(R.drawable.return_icon))
                )
            )
        }
        //endregion


        Spacer(
            modifier = Modifier
                .fillMaxWidth()
                .windowInsetsBottomHeight(WindowInsets.navigationBars)
        )
    }
}


private const val MINUTE_IN_MILLISECONDS = 60000L
private const val REPEATABLE_ACTION_TIME_DELAY = 60L
private const val keyBorderWidth = 1f
private const val keyShape = 5f
private const val minKeyHeight = 40f
private const val keyWidthPadding = 8f
private const val vibrateOnClick = true
private const val soundOnClick = false
@Composable
fun Key(
    key: KeyItem,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val context = LocalContext.current
    val ime = context as MdIMEService
    val coroutineScope = rememberCoroutineScope()
    var longClickPressed by remember { mutableStateOf(false) }
    val view = LocalView.current
    val audioManager = context.getSystemService(AUDIO_SERVICE) as AudioManager
    val backgroundColor =
        if (!isPressed) MaterialTheme.colorScheme.secondary
        else MaterialTheme.colorScheme.primary
    val keyColor =
        if (!isPressed) MaterialTheme.colorScheme.onSecondary
        else MaterialTheme.colorScheme.onPrimary
    val keyBorderColor = MaterialTheme.colorScheme.outline

    fun soundAndVibrate() {
        if (vibrateOnClick) view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
        if (soundOnClick) audioManager.playSoundEffect(AudioManager.FX_KEY_CLICK, 0.1f)
    }

    fun onLongClick() {
        if (key.keyAction != Done) {
            longClickPressed = true
            coroutineScope.launch(Dispatchers.IO) {
                withTimeout(MINUTE_IN_MILLISECONDS) {
                    while (true) {
                        performKeyAction(key.keyAction, ime)
                        delay(REPEATABLE_ACTION_TIME_DELAY)
                    }
                }
            }
        } else {
            performKeyAction(key.keyAction, ime)
        }
        soundAndVibrate()
    }

    LaunchedEffect(isPressed, longClickPressed) {
        if (isPressed) soundAndVibrate()
        else if (longClickPressed) {
            coroutineScope.coroutineContext.cancelChildren()
            longClickPressed = false
        }
    }

    Box(
        modifier = modifier
            .defaultMinSize(minHeight = minKeyHeight.dp)
            .clip(RoundedCornerShape(keyShape.dp))
            .border(keyBorderWidth.dp, keyBorderColor, shape = RoundedCornerShape(keyShape.dp))
            .background(color = backgroundColor)
            .combinedClickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = {
                    performKeyAction(key.keyAction, ime)
                    onClick?.invoke()
                },
                onLongClick = { onLongClick() }
            )
    ) {
        when (val type = key.keyType) {
            is KeyText -> {
                Text(
                    text = type.value,
                    fontSize = 22.sp,
                    color = keyColor,
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(horizontal = keyWidthPadding.dp)
                )
                if (type.showDescription) {
                    //TODO: ADD DESCRIPTION
                }
            }

            is KeyIcon -> {
                Icon(
                    imageVector = type.icon,
                    contentDescription = null, //TODO: ADD DESCRIPTION
                    tint = keyColor,
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(horizontal = keyWidthPadding.dp)
                )
            }
        }
    }
}

fun performKeyAction(
    action: KeyAction,
    ime: MdIMEService
) {
    val conn = ime.currentInputConnection
    when (action) {
        is CommitText -> {
            val text = action.text
            conn.commitText(text, 1)
        }
        Delete -> {
            //region TEXT STYLE
            val stylingSurroundingText = conn.getSurroundingText(2, 2, 0)
            if (stylingSurroundingText != null) {
                val text = stylingSurroundingText.text
                val prefix = text.subSequence(0, stylingSurroundingText.selectionStart)
                val suffix = text.subSequence(stylingSurroundingText.selectionEnd, text.length)

                if (prefix.isNotEmpty() && suffix.isNotEmpty()) {
                    if (prefix == BOLD && suffix == BOLD) {
                        conn.deleteSurroundingText(2, 2)
                        return
                    }

                    val lastPrefix = prefix.last().toString()
                    val firstSuffix = prefix.first().toString()
                    if ((lastPrefix == ITALIC || lastPrefix == CODE) && (firstSuffix == ITALIC || firstSuffix == CODE)) {
                        conn.deleteSurroundingText(1, 1)
                        return
                    }
                }
            }
            //endregion

            //region LIST
            val listSurroundingText = conn.getSurroundingText(6, 0, 0)
            if (listSurroundingText != null) {
                val prefix = listSurroundingText
                    .text
                    .subSequence(0, listSurroundingText.selectionStart)
                    .split("\n")
                    .last()

                if (prefix.isNotEmpty()) {
                    if (prefix == UNORDERED_LIST) {
                        conn.deleteSurroundingText(UNORDERED_LIST.length, 0)
                        return
                    }

                    if (prefix == ORDERED_LIST) {
                        conn.deleteSurroundingText(ORDERED_LIST.length, 0)
                        return
                    }

                    if (prefix == CHECKBOX || prefix == CHECKBOX.dropLast(1)) {
                        conn.deleteSurroundingText(CHECKBOX.length, 0)
                        return
                    }
                }
            }
            //endregion
            val event = KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_DEL)
            conn.sendKeyEvent(event)
        }
        Done -> {
            ime.requestHideSelf(0)
        }
        Enter -> {
            conn.beginBatchEdit()
            conn.commitText("\n", 1)

            val previousLine = conn
                .getSurroundingText(100, 0, 0)
                ?.text
                ?.split("\n")
                ?.let { it.getOrNull(it.lastIndex) }

            if (previousLine != null) {
                val stringBuilder = StringBuilder()
                val indentCount = previousLine.takeWhile { it == ' ' }.length
                for (i in 0..<indentCount step 4) {
                    stringBuilder.append("    ")
                }

                val listPrefix = previousLine
                    .dropWhile { it == ' ' }
                    .take(CHECKBOX.length)
                if (listPrefix.contains(CHECKBOX)) stringBuilder.append(CHECKBOX)
                else if (listPrefix.contains(UNORDERED_LIST)) stringBuilder.append(UNORDERED_LIST)
                else if (listPrefix.isOrderedList()) stringBuilder.append(listPrefix.nextElement())

                conn.commitText(stringBuilder, 1)
            }
            conn.endBatchEdit()
        }
        BoldSelection -> {
            conn.beginBatchEdit()
            val selectedText = conn.getSelectedText(0)
            if (selectedText.isNullOrEmpty()) {
                conn.commitText(BOLD, 1)
                conn.commitText(BOLD, 0)
            } else {
                conn.commitText("", 1)
                conn.commitText(BOLD, 1)
                conn.commitText(selectedText, 1)
                conn.commitText(BOLD, 0)
            }
            conn.endBatchEdit()
        }
        CursiveSelection -> {
            conn.beginBatchEdit()
            val selectedText = conn.getSelectedText(0)
            if (selectedText.isNullOrEmpty()) {
                conn.commitText(ITALIC, 1)
                conn.commitText(ITALIC, 0)
            } else {
                conn.commitText("", 1)
                conn.commitText(ITALIC, 1)
                conn.commitText(selectedText, 1)
                conn.commitText(ITALIC, 0)
            }

            conn.endBatchEdit()
        }
        IndentForward -> {
            conn.beginBatchEdit()
            val prefix = conn.getTextBeforeCursor(100, 0)
            val thisLine = prefix?.getThisLine()
            if (thisLine != null) {
                conn.deleteSurroundingText(thisLine.length, 0)
                val newLine = thisLine.replaceFirst("", "    ")
                conn.commitText(newLine, 1)
            }
            conn.endBatchEdit()
        }
        IndentBack -> {
            conn.beginBatchEdit()
            val prefix = conn.getTextBeforeCursor(100, 0)
            val thisLine = prefix?.getThisLine()
            if (thisLine != null) {
                conn.deleteSurroundingText(thisLine.length, 0)
                val newLine = thisLine.replaceFirst("    ", "")
                conn.commitText(newLine, 1)
            }
            conn.endBatchEdit()
        }
        CodeSelection -> {
            conn.beginBatchEdit()
            val selectedText = conn.getSelectedText(0)
            if (selectedText.isNullOrEmpty()) {
                conn.commitText(CODE, 1)
                conn.commitText(CODE, 0)
            } else {
                conn.commitText("", 1)
                conn.commitText(CODE, 1)
                conn.commitText(selectedText, 1)
                conn.commitText(CODE, 0)
            }
            conn.endBatchEdit()
        }
        Image -> TODO()
        Link -> TODO()
        Table -> TODO()
        Empty -> {}
    }
}

fun CharSequence.getThisLine(): String {
    return this.split("\n").last()
}

fun String.isOrderedList(): Boolean {
    return this.first().isDigit() &&
           this.getOrNull(1) == '.' &&
           this.getOrNull(2) == ' '
}

/**
 * Tries to increment the given string, if not possible it will return the given string
 */
fun String.nextElement(): String {
    val firstDigit = this.first { it.isDigit() }
    val newValue = firstDigit.digitToIntOrNull()?.plus(1)?.digitToChar() ?: return this
    val result =  this.replaceFirst(firstDigit, newValue)
    return result
}

data class KeyItem(val keyAction: KeyAction, val keyType: KeyType)

sealed interface KeyAction {
    data class CommitText(val text: String) : KeyAction
    data object Delete : KeyAction
    data object Done : KeyAction
    data object Enter : KeyAction
    data object IndentForward : KeyAction
    data object IndentBack : KeyAction
    data object BoldSelection : KeyAction
    data object CursiveSelection : KeyAction
    data object CodeSelection : KeyAction
    data object Table : KeyAction
    data object Link : KeyAction
    data object Image : KeyAction
    data object Empty : KeyAction
}

sealed class KeyType(
    open val description: Int? = null,
    open val showDescription: Boolean = false
) {
    data class KeyIcon(
        val icon: ImageVector,
        override val description: Int? = null,
        override val showDescription: Boolean = false
    ) : KeyType(description, showDescription)

    data class KeyText(
        val value: String,
        override val description: Int? = null,
        override val showDescription: Boolean = false
    ) : KeyType(description, showDescription)
}