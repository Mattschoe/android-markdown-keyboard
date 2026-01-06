package com.creategoodthings.markdownKeyboard.editor.rules

import com.creategoodthings.markdownKeyboard.editor.ContextNeed
import com.creategoodthings.markdownKeyboard.editor.InlineStyle
import com.creategoodthings.markdownKeyboard.editor.KeyAction
import com.creategoodthings.markdownKeyboard.editor.Snapshot
import com.creategoodthings.markdownKeyboard.editor.TextEdit
import com.creategoodthings.markdownKeyboard.editor.document.InlineSpans
import com.creategoodthings.markdownKeyboard.editor.document.Line
import com.creategoodthings.markdownKeyboard.editor.document.LineParser
import com.creategoodthings.markdownKeyboard.editor.document.LineType
import com.creategoodthings.markdownKeyboard.editor.document.Skeletons
import com.creategoodthings.markdownKeyboard.editor.document.lineAt

/**
 * Bold, italic, code and strikethrough, as toggles rather than as inserts.
 *
 * A selection is wrapped, or unwrapped if it already carries the delimiters. A caret inside an
 * existing span unwraps that span; a caret inside an *empty* pair takes that pair back out again;
 * anything else opens an empty pair with the caret between the halves.
 *
 * Code has one more rung: an empty pair is a block the user has not opened yet, so it escalates
 * to a fence, and pressing the key inside that empty block folds it back down to a code span.
 */
object InlineStyleHandler : KeyHandler {
    override fun contextNeed(action: KeyAction): ContextNeed? =
        if (action is KeyAction.ToggleInlineStyle) ContextNeed.CurrentLine else null

    override fun handle(snapshot: Snapshot, action: KeyAction): TextEdit? {
        val style = (action as? KeyAction.ToggleInlineStyle)?.style ?: return null
        val delimiter = style.delimiter

        if (snapshot.hasSelection) return toggleSelection(snapshot, delimiter)

        val caret = snapshot.selectionStart
        if (style == InlineStyle.Code) codeBlock(snapshot, caret)?.let { return it.edit }

        val lineStart = snapshot.lineStartAt(caret)
        val lineEnd = snapshot.lineEndAt(caret)
        val lineText = snapshot.text.substring(lineStart, lineEnd)
        val column = caret - lineStart

        InlineSpans.spanAt(lineText, delimiter, column)?.let { span ->
            val stripped = lineText.substring(0, span.openStart) +
                lineText.substring(span.contentStart, span.contentEnd) +
                lineText.substring(span.closeEnd)
            val width = delimiter.length
            val newColumn = when {
                column <= span.openStart -> column
                column >= span.closeEnd -> column - 2 * width
                else -> (column - width).coerceIn(span.openStart, span.contentEnd - width)
            }
            return TextEdit.replacingRegion(snapshot, lineStart, lineEnd, stripped, lineStart + newColumn)
        }

        emptyPairEdit(snapshot, lineStart, lineText, column, style)?.let { return it }

        return TextEdit(caret, caret, delimiter + delimiter, caret + delimiter.length)
    }

    /**
     * The caret standing in the middle of a run of delimiters, with nothing between them.
     *
     * `InlineSpans` deliberately refuses to read a run of four backticks as two exact pairs — that
     * is what keeps `**` from parsing as two italics — so an empty pair never turns up as a span
     * and has to be recognised here, from the runs on either side of the caret.
     *
     * The run is a stack of styles the user opened and has not typed into yet, read outside in:
     * `***|***` is a bold pair with an italic inside it. Pressing a style that is already in the
     * stack takes that one delimiter back out, wherever in the stack it sits, and leaves the rest
     * standing — bold, italic, bold ends on `*|*`. Pressing one that is not there opens it, the
     * ordinary way. Code is the exception: the whole run escalates into a fenced block.
     */
    private fun emptyPairEdit(
        snapshot: Snapshot,
        lineStart: Int,
        lineText: String,
        column: Int,
        style: InlineStyle,
    ): TextEdit? {
        val char = style.delimiter[0]
        var left = 0
        while (column - left - 1 >= 0 && lineText[column - left - 1] == char) left++
        var right = 0
        while (column + right < lineText.length && lineText[column + right] == char) right++
        if (left == 0 || left != right) return null

        val start = lineStart + column - left
        val end = lineStart + column + right
        if (style == InlineStyle.Code) {
            return InsertionHandler.fencedBlock(snapshot, start, end, body = "")
        }

        val stack = delimiterStack(char, left) ?: return null
        if (style.delimiter !in stack) return null

        val remaining = left - style.delimiter.length
        return TextEdit(
            replaceStart = start,
            replaceEnd = end,
            replacement = char.toString().repeat(remaining * 2),
            newSelectionStart = start + remaining,
        )
    }

    /**
     * A run of [length] repetitions of [char] as the delimiters it is made of, outermost first,
     * or null when it does not divide into delimiters at all.
     *
     * Longest first, which is how the run was written: `**` is a bold pair, `***` is that pair
     * with an italic inside it, `****` is two bold pairs.
     */
    private fun delimiterStack(char: Char, length: Int): List<String>? {
        val delimiters = InlineStyle.entries
            .map { it.delimiter }
            .filter { it[0] == char }
            .sortedByDescending { it.length }

        val stack = ArrayList<String>()
        var remaining = length
        while (remaining > 0) {
            val next = delimiters.firstOrNull { it.length <= remaining } ?: return null
            stack += next
            remaining -= next.length
        }
        return stack
    }

    /** A `null` edit is still an answer here — "the key is spoken for, and does nothing". */
    private class Answer(val edit: TextEdit?)

    /**
     * What the code key does when the caret is already in a fenced block.
     *
     * An empty block folds back down to an inline pair, so the key walks the same ladder in both
     * directions: nothing, a code span, a block, a code span again. A block with code in it is
     * already literal text, so the key is claimed but does nothing rather than littering the
     * user's code with backticks. Anywhere else, [codeBlock] declines and the caret rules run.
     */
    private fun codeBlock(snapshot: Snapshot, caret: Int): Answer? {
        val lines = LineParser.parse(snapshot.text)
        val caretLine = lines.lineAt(caret) ?: return null

        val empty = Skeletons.emptyFencedBlockAround(lines, caretLine)
        if (empty != null &&
            Skeletons.runIsFullyVisible(lines, empty, snapshot.reachedStart, snapshot.reachedEnd)
        ) {
            val delimiter = InlineStyle.Code.delimiter
            return Answer(
                TextEdit(
                    replaceStart = empty.first,
                    replaceEnd = empty.last + 1,
                    replacement = delimiter + delimiter,
                    newSelectionStart = empty.first + delimiter.length,
                ),
            )
        }

        return if (insideFencedBlock(snapshot, lines, caretLine)) Answer(null) else null
    }

    /**
     * Whether the caret sits inside a fenced block, by counting the fences above it.
     *
     * Only the lines *above* are counted, deliberately: `` ``|`` `` is a line of four backticks
     * that `LineParser` reads as a fence, and it is exactly the abandoned pair the code key is
     * meant to escalate.
     *
     * With the top of the window out of sight the count says nothing, so the key falls back to
     * its ordinary behaviour rather than guessing at parity it cannot see.
     */
    private fun insideFencedBlock(snapshot: Snapshot, lines: List<Line>, caretLine: Line): Boolean {
        if (!snapshot.reachedStart) return false
        val fencesAbove = (0 until caretLine.index).count { lines[it].type is LineType.CodeFence }
        return fencesAbove % 2 == 1
    }

    private fun toggleSelection(snapshot: Snapshot, delimiter: String): TextEdit {
        val selected = snapshot.selectedText
        val start = snapshot.selectionStart
        val alreadyWrapped = selected.length >= 2 * delimiter.length &&
            selected.startsWith(delimiter) && selected.endsWith(delimiter)

        if (alreadyWrapped) {
            val inner = selected.substring(delimiter.length, selected.length - delimiter.length)
            return TextEdit(start, snapshot.selectionEnd, inner, start, start + inner.length)
        }
        return TextEdit(
            replaceStart = start,
            replaceEnd = snapshot.selectionEnd,
            replacement = delimiter + selected + delimiter,
            newSelectionStart = start + delimiter.length,
            newSelectionEnd = start + delimiter.length + selected.length,
        )
    }
}
