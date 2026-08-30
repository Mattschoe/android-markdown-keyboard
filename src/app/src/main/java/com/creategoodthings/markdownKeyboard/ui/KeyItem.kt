package com.creategoodthings.markdownKeyboard.ui

import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.TextUnit
import com.creategoodthings.markdownKeyboard.editor.KeyAction
import com.creategoodthings.markdownKeyboard.ui.theme.KeyTone

/**
 * One key: what it does, what it looks like, and how it behaves when held.
 *
 * A key with a [longPressAction] fires that once on hold; a [repeatable] one repeats its own
 * action while held. The two are exclusive, and a key that is neither simply fires once.
 *
 * @param tone how prominently the key is painted. Defaults to [KeyTone.Primary] because most
 *   keys put something in the document; the ones that operate the keyboard say so explicitly.
 */
data class KeyItem(
    val action: KeyAction,
    val label: KeyLabel,
    val longPressAction: KeyAction? = null,
    val repeatable: Boolean = false,
    val tone: KeyTone = KeyTone.Primary,
)

sealed interface KeyLabel {
    /** String resource read out for accessibility. */
    val description: Int?

    /**
     * @param fontSize overrides the standard key label size, for labels that are words rather
     *   than single characters. Unspecified means the standard size.
     */
    data class Text(
        val value: String,
        override val description: Int? = null,
        val fontSize: TextUnit = TextUnit.Unspecified,
    ) : KeyLabel

    data class Icon(val image: ImageVector, override val description: Int? = null) : KeyLabel
}
