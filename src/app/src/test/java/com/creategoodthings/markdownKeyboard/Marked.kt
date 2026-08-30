package com.creategoodthings.markdownKeyboard

import com.creategoodthings.markdownKeyboard.editor.Snapshot

/**
 * Before/after strings with the caret and selection written into them.
 *
 * `▮` is the caret. `«…»` is a selection, and its end doubles as the caret. Every rule case in
 * the suite is a pair of these, which keeps a test readable as the thing the user would see.
 */
object Marked {
    const val CARET = '▮'
    const val SELECT_OPEN = '«'
    const val SELECT_CLOSE = '»'

    /** What [assertKey] expects when `MarkdownEngine.edit` correctly returns null. */
    const val NO_EDIT = "<no edit>"

    data class Positioned(val text: String, val start: Int, val end: Int)

    fun parse(marked: String): Positioned {
        val caret = marked.indexOf(CARET)
        val open = marked.indexOf(SELECT_OPEN)
        val close = marked.indexOf(SELECT_CLOSE)

        require(marked.count { it == CARET } <= 1) { "more than one caret in: $marked" }

        return when {
            open >= 0 && close > open -> {
                require(caret < 0) { "mark a selection or a caret, not both: $marked" }
                Positioned(strip(marked), open, close - 1)
            }

            caret >= 0 -> Positioned(strip(marked), caret, caret)
            else -> error("no caret or selection marked in: $marked")
        }
    }

    fun render(text: String, start: Int, end: Int): String =
        if (start == end) {
            text.substring(0, start) + CARET + text.substring(start)
        } else {
            text.substring(0, start) + SELECT_OPEN + text.substring(start, end) +
                SELECT_CLOSE + text.substring(end)
        }

    /**
     * A snapshot of the whole document, so nothing is truncated. `windowStart = 0` because the
     * window *is* the field here; [snapshotOf] covers the truncated cases.
     */
    fun snapshot(marked: String): Snapshot {
        val (text, start, end) = parse(marked)
        return Snapshot(text, start, end, windowStart = 0)
    }

    private fun strip(marked: String): String =
        marked.filterNot { it == CARET || it == SELECT_OPEN || it == SELECT_CLOSE }
}

/** A snapshot that admits it is only a window, for the cases in `TruncationTest`. */
fun snapshotOf(
    text: String,
    caret: Int,
    reachedStart: Boolean,
    reachedEnd: Boolean,
    windowStart: Int = 0,
): Snapshot = Snapshot(text, caret, caret, reachedStart, reachedEnd, windowStart)
