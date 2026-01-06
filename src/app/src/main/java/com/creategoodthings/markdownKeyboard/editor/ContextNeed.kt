package com.creategoodthings.markdownKeyboard.editor

/**
 * How much of the document a rule needs to see.
 *
 * Declaring it up front is what replaces the scattered fixed-size reads the keyboard used to
 * do: the reader fetches exactly this much, once, and rules never guess at a window size.
 */
sealed interface ContextNeed {
    /** Nothing to read. The key commits blind, so ordinary typing costs no round trip. */
    data object None : ContextNeed

    /** Just the line under the cursor. */
    data object CurrentLine : ContextNeed

    /** The whole contiguous list the cursor stands in, however far it runs. */
    data object EnclosingBlock : ContextNeed

    data class Window(val before: Int, val after: Int) : ContextNeed
}
