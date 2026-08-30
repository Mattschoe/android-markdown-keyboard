package com.creategoodthings.markdownKeyboard.ui

import android.os.SystemClock
import android.view.ViewConfiguration
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeGestures
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.windowInsetsBottomHeight
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.creategoodthings.markdownKeyboard.R
import com.creategoodthings.markdownKeyboard.editor.InlineStyle
import com.creategoodthings.markdownKeyboard.editor.KeyAction
import com.creategoodthings.markdownKeyboard.editor.ListKind
import com.creategoodthings.markdownKeyboard.ui.theme.KeyTone
import com.creategoodthings.markdownKeyboard.ui.theme.LocalKeyboardColors

private val LETTER_ROW_TOP = listOf("q", "w", "e", "r", "t", "y", "u", "i", "o", "p")
private val LETTER_ROW_MIDDLE = listOf("a", "s", "d", "f", "g", "h", "j", "k", "l")
private val LETTER_ROW_BOTTOM = listOf("z", "x", "c", "v", "b", "n", "m")

private const val SPACE_BETWEEN_ROWS = 10f
private const val SPACE_BETWEEN_KEYS = 2.5f

/**
 * Breathing room down the left and right edges, so the outer keys do not run into the screen
 * edge. Paid for out of the padding inside each key (see `KEY_HORIZONTAL_PADDING`), which keeps
 * the room left for labels unchanged.
 */
private const val KEYBOARD_EDGE_PADDING = 5f

/**
 * Breathing room above the first row. Matched to [SPACE_BETWEEN_ROWS] so the band over the top
 * row reads as one more gap in the same rhythm, rather than the row being clipped by the top of
 * the keyboard window. The bottom edge needs no equivalent: it is set by the system affordance
 * inset instead (see [systemAffordanceInsets]).
 */
private const val KEYBOARD_TOP_PADDING = SPACE_BETWEEN_ROWS

/** The `?123` label is a word, not a character, so it is set smaller than a letter key. */
private const val SYMBOLS_FONT_SIZE = 16f

/** Combining long stroke overlay: renders the label as a struck-through S. */
private const val STRIKETHROUGH_LABEL = "S̶"

@Composable
fun MarkdownKeyboard(onAction: (KeyAction) -> Unit) {
    var shift by remember { mutableStateOf(ShiftState.Off) }
    var lastShiftTapMs by remember { mutableLongStateOf(0L) }
    val doubleTapTimeoutMs = remember { ViewConfiguration.getDoubleTapTimeout().toLong() }

    fun onShiftTap() {
        val now = SystemClock.uptimeMillis()
        shift = shift.onTap(now - lastShiftTapMs, doubleTapTimeoutMs)
        lastShiftTapMs = now
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(LocalKeyboardColors.current.background)
            .padding(
                start = KEYBOARD_EDGE_PADDING.dp,
                end = KEYBOARD_EDGE_PADDING.dp,
                top = KEYBOARD_TOP_PADDING.dp,
            )
    ) {
        KeyRow {
            Key(
                KeyItem(
                    action = KeyAction.ToggleInlineStyle(InlineStyle.Bold),
                    label = KeyLabel.Icon(icon(R.drawable.bold_icon), R.string.key_bold),
                ),
                onAction, Modifier.weight(1f),
            )
            Key(
                KeyItem(
                    action = KeyAction.ToggleInlineStyle(InlineStyle.Italic),
                    label = KeyLabel.Icon(icon(R.drawable.italic_icon), R.string.key_italic),
                ),
                onAction, Modifier.weight(1f),
            )
            Key(
                KeyItem(
                    action = KeyAction.ToggleInlineStyle(InlineStyle.Strikethrough),
                    label = KeyLabel.Text(STRIKETHROUGH_LABEL, R.string.key_strikethrough),
                ),
                onAction, Modifier.weight(1f),
            )
            Key(
                KeyItem(
                    action = KeyAction.ToggleInlineStyle(InlineStyle.Code),
                    label = KeyLabel.Icon(icon(R.drawable.code_icon), R.string.key_code),
                    longPressAction = KeyAction.InsertCodeBlock,
                ),
                onAction, Modifier.weight(1f),
            )
            Key(
                KeyItem(
                    action = KeyAction.CycleHeading,
                    label = KeyLabel.Icon(icon(R.drawable.heading_icon), R.string.key_heading),
                    longPressAction = KeyAction.InsertHorizontalRule,
                ),
                onAction, Modifier.weight(1f),
            )
            Key(
                KeyItem(
                    action = KeyAction.ToggleQuote,
                    label = KeyLabel.Icon(icon(R.drawable.quote_icon), R.string.key_quote),
                ),
                onAction, Modifier.weight(1f),
            )
        }
        RowGap()

        KeyRow {
            Key(
                KeyItem(
                    action = KeyAction.ToggleList(ListKind.Bullet),
                    label = KeyLabel.Icon(icon(R.drawable.unordered_list_icon), R.string.key_bullet_list),
                ),
                onAction, Modifier.weight(1f),
            )
            Key(
                KeyItem(
                    action = KeyAction.ToggleList(ListKind.Ordered),
                    label = KeyLabel.Icon(icon(R.drawable.ordered_list_icon), R.string.key_ordered_list),
                    longPressAction = KeyAction.NormalizeList,
                ),
                onAction, Modifier.weight(1f),
            )
            Key(
                KeyItem(
                    action = KeyAction.ToggleList(ListKind.Task),
                    label = KeyLabel.Icon(icon(R.drawable.checkbox_icon), R.string.key_task_list),
                ),
                onAction, Modifier.weight(1f),
            )
            Key(
                KeyItem(
                    action = KeyAction.InsertLink,
                    label = KeyLabel.Icon(icon(R.drawable.link_icon), R.string.key_link),
                ),
                onAction, Modifier.weight(1f),
            )
            Key(
                KeyItem(
                    action = KeyAction.InsertImage,
                    label = KeyLabel.Icon(icon(R.drawable.image_icon), R.string.key_image),
                ),
                onAction, Modifier.weight(1f),
            )
            Key(
                KeyItem(
                    action = KeyAction.InsertTable,
                    label = KeyLabel.Icon(icon(R.drawable.table_icon), R.string.key_table),
                ),
                onAction, Modifier.weight(1f),
            )
        }
        RowGap()

        KeyRow {
            LetterKeys(LETTER_ROW_TOP, shift, onAction) { shift = shift.afterCharacter() }
        }
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
            LetterKeys(LETTER_ROW_MIDDLE, shift, onAction) { shift = shift.afterCharacter() }
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
                onClick = { onShiftTap() },
            )
            LetterKeys(LETTER_ROW_BOTTOM, shift, onAction) { shift = shift.afterCharacter() }
            Key(
                KeyItem(
                    action = KeyAction.Backspace,
                    label = KeyLabel.Icon(icon(R.drawable.delete_icon), R.string.key_backspace),
                    repeatable = true,
                    tone = KeyTone.Utility,
                ),
                onAction, Modifier.weight(1.5f),
            )
        }
        RowGap()

        // Every key in this row is weighted, so the space bar can be held to a set share of the
        // width instead of swallowing whatever the others leave over.
        KeyRow {
            Key(
                KeyItem(
                    action = KeyAction.Noop,
                    label = KeyLabel.Text("?123", R.string.key_symbols, SYMBOLS_FONT_SIZE.sp),
                    tone = KeyTone.Utility,
                ),
                onAction, Modifier.weight(1.3f),
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
            Key(
                KeyItem(KeyAction.CommitText(" "), KeyLabel.Text(" "), repeatable = true),
                onAction,
                Modifier.weight(3.8f),
            )
            Key(
                KeyItem(KeyAction.CommitText("."), KeyLabel.Text("."), tone = KeyTone.Utility),
                onAction, Modifier.weight(1.15f),
            )
            Key(
                KeyItem(
                    action = KeyAction.Enter,
                    label = KeyLabel.Icon(icon(R.drawable.return_icon), R.string.key_enter),
                    tone = KeyTone.Accent,
                ),
                onAction, Modifier.weight(1.7f),
            )
        }

        Spacer(
            modifier = Modifier
                .fillMaxWidth()
                .windowInsetsBottomHeight(systemAffordanceInsets())
        )
    }
}

/**
 * The strip along the bottom that the OS keeps for itself.
 *
 * `navigationBars` alone is not enough: the IME window gets a *reduced* navigation bar inset
 * (24dp on gesture navigation) while the system still paints and taps its own affordances —
 * the IME switcher, the OEM's voice button — in a taller band (48dp) that hangs over the
 * bottom key row. `safeGestures` reports that taller band (`systemGestures` +
 * `mandatorySystemGestures` + `tappableElement`), so the union of the two is the first row
 * of pixels a key may safely occupy under either navigation mode.
 */
@Composable
private fun systemAffordanceInsets(): WindowInsets =
    WindowInsets.safeGestures.union(WindowInsets.navigationBars)

@Composable
private fun RowScope.LetterKeys(
    letters: List<String>,
    shift: ShiftState,
    onAction: (KeyAction) -> Unit,
    onTyped: () -> Unit,
) {
    for (letter in letters) {
        val value = if (shift.isUpperCase) letter.uppercase() else letter
        Key(
            key = KeyItem(KeyAction.CommitText(value), KeyLabel.Text(value)),
            onAction = onAction,
            modifier = Modifier.weight(1f),
            onClick = onTyped,
        )
    }
}

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

@Composable
private fun KeyRow(content: @Composable RowScope.() -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(SPACE_BETWEEN_KEYS.dp),
        content = content,
    )
}

@Composable
private fun ColumnScope.RowGap() = Spacer(Modifier.height(SPACE_BETWEEN_ROWS.dp))

@Composable
private fun icon(id: Int): ImageVector = ImageVector.vectorResource(id)
