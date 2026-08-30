package com.creategoodthings.markdownKeyboard.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.creategoodthings.markdownKeyboard.editor.KeyAction
import com.creategoodthings.markdownKeyboard.ui.theme.KeyTone

internal const val SPACE_BETWEEN_ROWS = 10f
internal const val SPACE_BETWEEN_KEYS = 2.5f

/**
 * Breathing room down the left and right edges, so the outer keys do not run into the screen
 * edge. Paid for out of the padding inside each key (see `KEY_HORIZONTAL_PADDING`), which keeps
 * the room left for labels unchanged.
 */
internal const val KEYBOARD_EDGE_PADDING = 5f

/**
 * Breathing room above the first row. Matched to [SPACE_BETWEEN_ROWS] so the band over the top
 * row reads as one more gap in the same rhythm, rather than the row being clipped by the top of
 * the keyboard window. The bottom edge needs no equivalent: it is set by the system affordance
 * inset instead.
 */
internal const val KEYBOARD_TOP_PADDING = SPACE_BETWEEN_ROWS

/** `?123`, `ABC`, `=\<` and friends are words, not characters, so they are set smaller. */
internal const val MODE_FONT_SIZE = 16f

@Composable
internal fun KeyRow(content: @Composable RowScope.() -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(SPACE_BETWEEN_KEYS.dp),
        content = content,
    )
}

@Composable
internal fun ColumnScope.RowGap() = Spacer(Modifier.height(SPACE_BETWEEN_ROWS.dp))

@Composable
internal fun icon(id: Int): ImageVector = ImageVector.vectorResource(id)

/**
 * A run of keys that each type one character.
 *
 * @param transform applied to the character and to each of its alternates, so a shifted letter
 *   row offers `É È Ê` rather than the lower case set.
 * @param onTyped runs after the character is committed; the letter rows use it to spend a
 *   one-shot shift.
 */
@Composable
internal fun RowScope.CharacterKeys(
    characters: List<String>,
    onAction: (KeyAction) -> Unit,
    weight: Float = 1f,
    tone: KeyTone = KeyTone.Primary,
    transform: (String) -> String = { it },
    alternates: (String) -> List<String> = { emptyList() },
    onTyped: (() -> Unit)? = null,
) {
    for (character in characters) {
        val value = transform(character)
        Key(
            key = KeyItem(
                action = KeyAction.CommitText(value),
                label = KeyLabel.Text(value),
                hold = holdFor(value, alternates(character).map(transform)),
                tone = tone,
            ),
            onAction = onAction,
            modifier = Modifier.weight(weight),
            onClick = onTyped,
        )
    }
}

/**
 * The hold behaviour for a character key.
 *
 * The character itself leads the strip, so it sits over its own key and a hold released without
 * sliding types exactly what a tap would have.
 */
internal fun holdFor(value: String, alternates: List<String>): HoldBehaviour =
    if (alternates.isEmpty()) HoldBehaviour.None
    else HoldBehaviour.Alternates(listOf(value) + alternates, baseIndex = 0)

/** A key that switches pages instead of typing: it commits nothing and acts in `onClick`. */
internal fun modeKey(label: String, description: Int): KeyItem = KeyItem(
    action = KeyAction.Noop,
    label = KeyLabel.Text(label, description, MODE_FONT_SIZE.sp),
    tone = KeyTone.Utility,
)
