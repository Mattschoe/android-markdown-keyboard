package com.creategoodthings.markdownKeyboard.ui

import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.RowScope
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import com.creategoodthings.markdownKeyboard.R
import com.creategoodthings.markdownKeyboard.editor.KeyAction
import com.creategoodthings.markdownKeyboard.ui.theme.KeyTone

private val LETTER_ROW_TOP = listOf("q", "w", "e", "r", "t", "y", "u", "i", "o", "p")
private val LETTER_ROW_MIDDLE = listOf("a", "s", "d", "f", "g", "h", "j", "k", "l")
private val LETTER_ROW_BOTTOM = listOf("z", "x", "c", "v", "b", "n", "m")

/** The QWERTY page: three letter rows and the bottom bar. */
@Composable
internal fun ColumnScope.LetterRows(
    shift: ShiftState,
    onAction: (KeyAction) -> Unit,
    onShiftTap: () -> Unit,
    onTyped: () -> Unit,
    onPage: (KeyboardPage) -> Unit,
) {
    KeyRow { LetterKeys(LETTER_ROW_TOP, shift, onAction, onTyped) }
    RowGap()

    KeyRow {
        Key(
            KeyItem(
                action = KeyAction.IndentForward,
                label = KeyLabel.Icon(icon(R.drawable.tab_in_icon), R.string.key_indent),
                tone = KeyTone.Utility,
            ),
            onAction, Modifier.weight(1.5f),
        )
        LetterKeys(LETTER_ROW_MIDDLE, shift, onAction, onTyped)
        Key(
            KeyItem(
                action = KeyAction.IndentBack,
                label = KeyLabel.Icon(icon(R.drawable.tab_out_icon), R.string.key_outdent),
                tone = KeyTone.Utility,
            ),
            onAction, Modifier.weight(1.5f),
        )
    }
    RowGap()

    KeyRow {
        Key(
            KeyItem(
                action = KeyAction.Noop,
                label = KeyLabel.Icon(shiftIcon(shift), shiftDescription(shift)),
                tone = KeyTone.Utility,
            ),
            onAction,
            Modifier.weight(1.5f),
            onClick = onShiftTap,
        )
        LetterKeys(LETTER_ROW_BOTTOM, shift, onAction, onTyped)
        BackspaceKey(onAction, Modifier.weight(1.5f))
    }
    RowGap()

    // Every key in this row is weighted, so the space bar can be held to a set share of the
    // width instead of swallowing whatever the others leave over.
    KeyRow {
        Key(
            modeKey("?123", R.string.key_symbols),
            onAction, Modifier.weight(1.3f),
            onClick = { onPage(KeyboardPage.Symbols) },
        )
        Key(
            KeyItem(
                action = KeyAction.Noop,
                label = KeyLabel.Icon(icon(R.drawable.emoji_icon), R.string.key_emoji),
                tone = KeyTone.Utility,
            ),
            onAction, Modifier.weight(0.9f),
        )
        Key(
            KeyItem(KeyAction.CommitText(","), KeyLabel.Text(","), tone = KeyTone.Utility),
            onAction, Modifier.weight(1.15f),
        )
        SpaceKey(onAction, Modifier.weight(3.8f))
        Key(
            KeyItem(
                action = KeyAction.CommitText("."),
                label = KeyLabel.Text("."),
                hold = holdFor(".", Alternates.symbol(".")),
                tone = KeyTone.Utility,
            ),
            onAction, Modifier.weight(1.15f),
        )
        EnterKey(onAction, Modifier.weight(1.7f))
    }
}

/** Letters carry accents on hold, and shift upper-cases the key and its accents together. */
@Composable
private fun RowScope.LetterKeys(
    letters: List<String>,
    shift: ShiftState,
    onAction: (KeyAction) -> Unit,
    onTyped: () -> Unit,
) = CharacterKeys(
    characters = letters,
    onAction = onAction,
    transform = { letter -> if (shift.isUpperCase) letter.uppercase() else letter },
    alternates = Alternates::letter,
    onTyped = onTyped,
)

@Composable
private fun shiftIcon(shift: ShiftState): ImageVector = icon(
    when (shift) {
        ShiftState.Off -> R.drawable.shift_icon
        ShiftState.Shifted -> R.drawable.shift_filled
        ShiftState.Locked -> R.drawable.caps_lock_filled
    }
)

private fun shiftDescription(shift: ShiftState): Int = when (shift) {
    ShiftState.Off -> R.string.key_shift
    ShiftState.Shifted -> R.string.key_shift_on
    ShiftState.Locked -> R.string.key_caps_lock
}

/** The three keys every page ends its bottom row with, so they cannot drift apart. */
@Composable
internal fun BackspaceKey(onAction: (KeyAction) -> Unit, modifier: Modifier) = Key(
    KeyItem(
        action = KeyAction.Backspace,
        label = KeyLabel.Icon(icon(R.drawable.delete_icon), R.string.key_backspace),
        hold = HoldBehaviour.Repeat,
        tone = KeyTone.Utility,
    ),
    onAction, modifier,
)

@Composable
internal fun SpaceKey(onAction: (KeyAction) -> Unit, modifier: Modifier) = Key(
    KeyItem(
        action = KeyAction.CommitText(" "),
        label = KeyLabel.Text(" ", R.string.key_space),
        hold = HoldBehaviour.Repeat,
    ),
    onAction, modifier,
)

@Composable
internal fun EnterKey(onAction: (KeyAction) -> Unit, modifier: Modifier) = Key(
    KeyItem(
        action = KeyAction.Enter,
        label = KeyLabel.Icon(icon(R.drawable.return_icon), R.string.key_enter),
        tone = KeyTone.Accent,
    ),
    onAction, modifier,
)

/** `ABC`, on every page but the letters. */
@Composable
internal fun LettersKey(onPage: (KeyboardPage) -> Unit, onAction: (KeyAction) -> Unit, modifier: Modifier) =
    Key(
        modeKey("ABC", R.string.key_letters),
        onAction, modifier,
        onClick = { onPage(KeyboardPage.Letters) },
    )
