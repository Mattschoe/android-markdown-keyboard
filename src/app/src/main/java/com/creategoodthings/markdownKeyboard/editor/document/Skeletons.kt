package com.creategoodthings.markdownKeyboard.editor.document

import com.creategoodthings.markdownKeyboard.editor.MarkdownSyntax

/**
 * Finds skeletons the user opened and then abandoned, so backspace can take one back whole.
 *
 * The mirror image of the insertion keys: what `MarkdownSyntax` writes is what this reads. Only
 * *empty* skeletons count — once anything has been typed into a link or a table it is ordinary
 * text again, and backspace goes back to deleting characters.
 */
object Skeletons {
    /** An empty row of the table skeleton: pipes with nothing but spaces between them. */
    private val EMPTY_ROW = Regex("""^\|(?:[ \t]*\|)+$""")

    /** `| --- | :-: |` and friends. */
    private val DELIMITER_ROW = Regex("""^\|(?:[ \t]*:?-+:?[ \t]*\|)+$""")

    /** Longest opener first, so `![]()` is never read as a `[]()` with a stray `!` in front. */
    private val INLINE_TARGETS = listOf(MarkdownSyntax.IMAGE_SKELETON, MarkdownSyntax.LINK_SKELETON)

    /**
     * The empty link or image on [line] that [column] stands inside, as a range of columns.
     *
     * A caret sitting immediately *before* the opening bracket is not inside it, so backspace
     * there stays an ordinary deletion of whatever precedes the skeleton.
     */
    fun emptyInlineTargetAt(line: String, column: Int): IntRange? {
        for (shape in INLINE_TARGETS) {
            var start = line.indexOf(shape)
            while (start >= 0) {
                val end = start + shape.length
                if (column in (start + 1)..end) return start until end
                start = line.indexOf(shape, start + 1)
            }
        }
        return null
    }

    /**
     * The run of empty table lines around [caretLine], as a range of offsets into the snapshot.
     *
     * A run only counts as a table skeleton if it carries a delimiter row; a lone `|  |  |` is
     * just a line the user typed.
     */
    fun emptyTableAround(lines: List<Line>, caretLine: Line): IntRange? {
        if (!isTableSkeletonRow(caretLine.text)) return null

        var first = caretLine.index
        while (first > 0 && isTableSkeletonRow(lines[first - 1].text)) first--
        var last = caretLine.index
        while (last < lines.lastIndex && isTableSkeletonRow(lines[last + 1].text)) last++

        val sawDelimiter = (first..last).any { DELIMITER_ROW.matches(lines[it].text) }
        if (!sawDelimiter) return null

        return lines[first].start until lines[last].end
    }

    /**
     * The empty fenced block around [caretLine], as a range of offsets into the snapshot.
     *
     * Empty means the fences have nothing but blank lines between them — once there is code in
     * there it is the user's, not a skeleton. A caret on the opening fence counts as inside;
     * one on the closing fence does not, since from there the block above is already finished.
     */
    fun emptyFencedBlockAround(lines: List<Line>, caretLine: Line): IntRange? {
        var open = caretLine.index
        while (open >= 0 && lines[open].type !is LineType.CodeFence) {
            if (!lines[open].text.isBlank()) return null
            open--
        }
        if (open < 0) return null

        var close = open + 1
        while (close <= lines.lastIndex && lines[close].type !is LineType.CodeFence) {
            if (!lines[close].text.isBlank()) return null
            close++
        }
        if (close > lines.lastIndex) return null

        val opening = lines[open].type as LineType.CodeFence
        val closing = lines[close].type as LineType.CodeFence
        if (opening.fence != closing.fence) return null

        return lines[open].start until lines[close].end
    }

    /** Whether a run found here is bounded by text we actually saw, rather than by the window. */
    fun runIsFullyVisible(
        lines: List<Line>,
        run: IntRange,
        reachedStart: Boolean,
        reachedEnd: Boolean,
    ): Boolean {
        val touchesTop = run.first <= lines.first().start
        val touchesBottom = run.last >= lines.last().end - 1
        return (reachedStart || !touchesTop) && (reachedEnd || !touchesBottom)
    }

    private fun isTableSkeletonRow(line: String): Boolean =
        EMPTY_ROW.matches(line) || DELIMITER_ROW.matches(line)
}
