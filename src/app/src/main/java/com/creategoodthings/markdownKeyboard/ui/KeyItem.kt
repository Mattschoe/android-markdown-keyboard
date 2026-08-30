package com.creategoodthings.markdownKeyboard.ui

import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.TextUnit
import com.creategoodthings.markdownKeyboard.editor.KeyAction
import com.creategoodthings.markdownKeyboard.ui.theme.KeyTone

/**
 * One key: what it does, what it looks like, and how it behaves when held.
 *
 * @param tone how prominently the key is painted. Defaults to [KeyTone.Primary] because most
 *   keys put something in the document; the ones that operate the keyboard say so explicitly.
 */
data class KeyItem(
    val action: KeyAction,
    val label: KeyLabel,
    val hold: HoldBehaviour = HoldBehaviour.None,
    val tone: KeyTone = KeyTone.Primary,
)

/**
 * What holding a key does. Exactly one of these, which is why it is a sealed type rather than a
 * nullable action plus a boolean plus a list: the gesture loop in [Key] wants a total `when`.
 */
sealed interface HoldBehaviour {
    /** Fires the key's own action a second time. */
    data object None : HoldBehaviour

    /** Fires a different action, once. */
    data class Action(val action: KeyAction) : HoldBehaviour

    /** Repeats the key's own action while held. */
    data object Repeat : HoldBehaviour

    /**
     * Pops a strip of characters above the key that the finger slides onto and releases over.
     *
     * [values] holds the key's own character as well as its alternates: the one at [baseIndex] is
     * parked over the key and is what a hold-and-release without sliding commits, so holding a key
     * by mistake costs nothing.
     */
    data class Alternates(val values: List<String>, val baseIndex: Int = 0) : HoldBehaviour
}

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
