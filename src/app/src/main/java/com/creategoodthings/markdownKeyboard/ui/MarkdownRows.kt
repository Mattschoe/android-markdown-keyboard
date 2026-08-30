package com.creategoodthings.markdownKeyboard.ui

import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.creategoodthings.markdownKeyboard.R
import com.creategoodthings.markdownKeyboard.editor.InlineStyle
import com.creategoodthings.markdownKeyboard.editor.KeyAction
import com.creategoodthings.markdownKeyboard.editor.ListKind

/** Combining long stroke overlay: renders the label as a struck-through S. */
private const val STRIKETHROUGH_LABEL = "S̶"

/**
 * The two rows that never change.
 *
 * Formatting is what the keyboard is for, so it stays one tap away whichever character page is
 * showing — which also means every page is the same height and the keyboard never resizes under
 * the user's thumb when they switch.
 */
@Composable
internal fun ColumnScope.MarkdownRows(onAction: (KeyAction) -> Unit) {
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
                hold = HoldBehaviour.Action(KeyAction.InsertCodeBlock),
            ),
            onAction, Modifier.weight(1f),
        )
        Key(
            KeyItem(
                action = KeyAction.CycleHeading,
                label = KeyLabel.Icon(icon(R.drawable.heading_icon), R.string.key_heading),
                hold = HoldBehaviour.Action(KeyAction.InsertHorizontalRule),
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
                label = KeyLabel.Icon(
                    icon(R.drawable.unordered_list_icon),
                    R.string.key_bullet_list,
                ),
            ),
            onAction, Modifier.weight(1f),
        )
        Key(
            KeyItem(
                action = KeyAction.ToggleList(ListKind.Ordered),
                label = KeyLabel.Icon(icon(R.drawable.ordered_list_icon), R.string.key_ordered_list),
                hold = HoldBehaviour.Action(KeyAction.NormalizeList),
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
}
