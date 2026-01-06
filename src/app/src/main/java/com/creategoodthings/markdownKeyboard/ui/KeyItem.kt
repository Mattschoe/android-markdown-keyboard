package com.creategoodthings.markdownKeyboard.ui

import androidx.compose.ui.graphics.vector.ImageVector
import com.creategoodthings.markdownKeyboard.editor.KeyAction

/**
 * One key: what it does, what it looks like, and how it behaves when held.
 *
 * A key with a [longPressAction] fires that once on hold; a [repeatable] one repeats its own
 * action while held. The two are exclusive, and a key that is neither simply fires once.
 */
data class KeyItem(
    val action: KeyAction,
    val label: KeyLabel,
    val longPressAction: KeyAction? = null,
    val repeatable: Boolean = false,
)

sealed interface KeyLabel {
    /** String resource read out for accessibility. */
    val description: Int?

    data class Text(val value: String, override val description: Int? = null) : KeyLabel

    data class Icon(val image: ImageVector, override val description: Int? = null) : KeyLabel
}
