package com.creategoodthings.markdownKeyboard.ui

import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.RowScope
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.creategoodthings.markdownKeyboard.R
import com.creategoodthings.markdownKeyboard.editor.KeyAction
import com.creategoodthings.markdownKeyboard.ui.theme.KeyTone

private val DIGITS = listOf("1", "2", "3", "4", "5", "6", "7", "8", "9", "0")
private val SYMBOLS_SECOND = listOf("@", "#", "$", "_", "&", "-", "+", "(", ")", "/")
private val SYMBOLS_THIRD = listOf("*", "\"", "'", ":", ";", "!", "?")

private val EXTRA_FIRST = listOf("~", "`", "|", "•", "√", "π", "÷", "×", "¶", "Δ")
private val EXTRA_SECOND = listOf("£", "¢", "€", "¥", "^", "°", "=", "{", "}", "\\")
private val EXTRA_THIRD = listOf("™", "®", "©", "℅", "±", "[", "]")

/** `?123`: the digits and the punctuation that earns a key of its own. */
@Composable
internal fun ColumnScope.SymbolRows(
    onAction: (KeyAction) -> Unit,
    onPage: (KeyboardPage) -> Unit,
) {
    KeyRow { SymbolKeys(DIGITS, onAction) }
    RowGap()

    KeyRow { SymbolKeys(SYMBOLS_SECOND, onAction) }
    RowGap()

    KeyRow {
        Key(
            modeKey("=\\<", R.string.key_more_symbols),
            onAction, Modifier.weight(1.5f),
            onClick = { onPage(KeyboardPage.SymbolsExtra) },
        )
        SymbolKeys(SYMBOLS_THIRD, onAction)
        BackspaceKey(onAction, Modifier.weight(1.5f))
    }
    RowGap()

    KeyRow {
        LettersKey(onPage, onAction, Modifier.weight(1.3f))
        SymbolKeys(listOf(","), onAction, weight = 0.9f, tone = KeyTone.Utility)
        Key(
            modeKey("123", R.string.key_numeric),
            onAction, Modifier.weight(1.15f),
            onClick = { onPage(KeyboardPage.Numeric) },
        )
        SpaceKey(onAction, Modifier.weight(3.8f))
        SymbolKeys(listOf("."), onAction, weight = 1.15f, tone = KeyTone.Utility)
        EnterKey(onAction, Modifier.weight(1.7f))
    }
}

/** `=\<`: currency, maths and the marks that did not fit on [SymbolRows]. */
@Composable
internal fun ColumnScope.SymbolsExtraRows(
    onAction: (KeyAction) -> Unit,
    onPage: (KeyboardPage) -> Unit,
) {
    KeyRow { SymbolKeys(EXTRA_FIRST, onAction) }
    RowGap()

    KeyRow { SymbolKeys(EXTRA_SECOND, onAction) }
    RowGap()

    KeyRow {
        Key(
            modeKey("?123", R.string.key_symbols),
            onAction, Modifier.weight(1.5f),
            onClick = { onPage(KeyboardPage.Symbols) },
        )
        SymbolKeys(EXTRA_THIRD, onAction)
        BackspaceKey(onAction, Modifier.weight(1.5f))
    }
    RowGap()

    KeyRow {
        LettersKey(onPage, onAction, Modifier.weight(1.3f))
        SymbolKeys(listOf(",", "<", ">"), onAction, weight = 0.9f, tone = KeyTone.Utility)
        SpaceKey(onAction, Modifier.weight(3.0f))
        SymbolKeys(listOf("."), onAction, weight = 1.3f, tone = KeyTone.Utility)
        EnterKey(onAction, Modifier.weight(1.7f))
    }
}

@Composable
internal fun RowScope.SymbolKeys(
    characters: List<String>,
    onAction: (KeyAction) -> Unit,
    weight: Float = 1f,
    tone: KeyTone = KeyTone.Primary,
) = CharacterKeys(
    characters = characters,
    onAction = onAction,
    weight = weight,
    tone = tone,
    alternates = Alternates::symbol,
)
