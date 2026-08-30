package com.creategoodthings.markdownKeyboard.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.creategoodthings.markdownKeyboard.R
import com.creategoodthings.markdownKeyboard.editor.KeyAction
import com.creategoodthings.markdownKeyboard.ui.theme.KeyTone

private val OPERATORS = listOf("+", "-", "*", "/")
private val DIGIT_GRID = listOf(
    listOf("1", "2", "3"),
    listOf("4", "5", "6"),
    listOf("7", "8", "9"),
)

/**
 * The block above the bottom row is three digit rows tall, and the operator column fits four keys
 * into that same height. Weighting inside a column needs a bounded height, so the block is given
 * one explicitly — the same arithmetic the three rows would have come to anyway.
 */
private const val BLOCK_HEIGHT = 3 * KEY_HEIGHT + 2 * SPACE_BETWEEN_ROWS

/** `1234`: the number pad, with the arithmetic operators down the left. */
@Composable
internal fun ColumnScope.NumericRows(
    onAction: (KeyAction) -> Unit,
    onPage: (KeyboardPage) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(BLOCK_HEIGHT.dp),
        horizontalArrangement = Arrangement.spacedBy(SPACE_BETWEEN_KEYS.dp),
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight(),
            verticalArrangement = Arrangement.spacedBy(SPACE_BETWEEN_KEYS.dp),
        ) {
            for (operator in OPERATORS) {
                NumericKey(operator, onAction, Modifier.weight(1f).fillMaxWidth(), KeyTone.Utility)
            }
        }

        Column(
            modifier = Modifier
                .weight(7.5f)
                .fillMaxHeight(),
            verticalArrangement = Arrangement.spacedBy(SPACE_BETWEEN_ROWS.dp),
        ) {
            for (digits in DIGIT_GRID) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(SPACE_BETWEEN_KEYS.dp),
                ) {
                    for (digit in digits) {
                        NumericKey(digit, onAction, Modifier.weight(1f).fillMaxHeight())
                    }
                }
            }
        }

        Column(
            modifier = Modifier
                .weight(1.25f)
                .fillMaxHeight(),
            verticalArrangement = Arrangement.spacedBy(SPACE_BETWEEN_ROWS.dp),
        ) {
            NumericKey("%", onAction, Modifier.weight(1f).fillMaxWidth())
            SpaceKey(onAction, Modifier.weight(1f).fillMaxWidth())
            BackspaceKey(onAction, Modifier.weight(1f).fillMaxWidth())
        }
    }
    RowGap()

    KeyRow {
        LettersKey(onPage, onAction, Modifier.weight(1.3f))
        SymbolKeys(listOf(","), onAction, weight = 1.0f, tone = KeyTone.Utility)
        Key(
            modeKey("!?#", R.string.key_symbols),
            onAction, Modifier.weight(1.3f),
            onClick = { onPage(KeyboardPage.Symbols) },
        )
        SymbolKeys(listOf("0"), onAction, weight = 2.5f)
        SymbolKeys(listOf("="), onAction, weight = 1.2f, tone = KeyTone.Utility)
        SymbolKeys(listOf("."), onAction, weight = 1.0f, tone = KeyTone.Utility)
        EnterKey(onAction, Modifier.weight(1.7f))
    }
}

@Composable
private fun NumericKey(
    character: String,
    onAction: (KeyAction) -> Unit,
    modifier: Modifier,
    tone: KeyTone = KeyTone.Primary,
) = Key(
    key = KeyItem(
        action = KeyAction.CommitText(character),
        label = KeyLabel.Text(character),
        hold = holdFor(character, Alternates.symbol(character)),
        tone = tone,
    ),
    onAction = onAction,
    modifier = modifier,
)
