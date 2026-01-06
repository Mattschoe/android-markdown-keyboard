package com.creategoodthings.markdownKeyboard.editor

/**
 * An immutable view of the editor's text around the cursor, read fresh for every contextual key.
 *
 * [text] is a *window*, not necessarily the whole field. [reachedStart] and [reachedEnd] say
 * whether that window ran all the way to the real edges of the document, which is how a rule
 * knows the difference between "there is no list below this" and "I could not see far enough".
 * Rules that would otherwise rewrite text they never saw must degrade instead of guessing.
 */
data class Snapshot(
    val text: String,
    val selectionStart: Int,
    val selectionEnd: Int,
    val reachedStart: Boolean = true,
    val reachedEnd: Boolean = true,
    /** Offset of [text] within the real field, or -1 when the editor would not say. */
    val windowStart: Int = -1,
) {
    init {
        require(selectionStart in 0..text.length) { "selectionStart out of bounds" }
        require(selectionEnd in selectionStart..text.length) { "selectionEnd out of bounds" }
    }

    val hasSelection: Boolean get() = selectionStart != selectionEnd

    val selectedText: String get() = text.substring(selectionStart, selectionEnd)

    fun lineStartAt(offset: Int): Int = text.lastIndexOf('\n', offset - 1) + 1

    fun lineEndAt(offset: Int): Int =
        text.indexOf('\n', offset).let { if (it < 0) text.length else it }

    fun lineTextAt(offset: Int): String = text.substring(lineStartAt(offset), lineEndAt(offset))
}
