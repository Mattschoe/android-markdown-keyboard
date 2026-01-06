package com.creategoodthings.markdownKeyboard.editor

/**
 * What a key means, independent of how it is drawn or how it reaches the editor.
 *
 * The UI emits these and knows nothing else about markdown.
 */
sealed interface KeyAction {
    /** Plain text with no context needed: letters, punctuation, space. */
    data class CommitText(val text: String) : KeyAction

    data object Enter : KeyAction
    data object Backspace : KeyAction
    data object Done : KeyAction

    data object IndentForward : KeyAction
    data object IndentBack : KeyAction

    data class ToggleInlineStyle(val style: InlineStyle) : KeyAction
    data class ToggleList(val kind: ListKind) : KeyAction

    data object CycleHeading : KeyAction
    data object ToggleQuote : KeyAction

    /** Renumbers the list the cursor stands in, without changing anything else. */
    data object NormalizeList : KeyAction

    data object InsertLink : KeyAction
    data object InsertImage : KeyAction
    data object InsertTable : KeyAction
    data object InsertCodeBlock : KeyAction
    data object InsertHorizontalRule : KeyAction

    data object Noop : KeyAction
}
