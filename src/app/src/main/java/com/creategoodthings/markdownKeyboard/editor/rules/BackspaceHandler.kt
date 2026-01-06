package com.creategoodthings.markdownKeyboard.editor.rules

import com.creategoodthings.markdownKeyboard.editor.ContextNeed
import com.creategoodthings.markdownKeyboard.editor.InlineStyle
import com.creategoodthings.markdownKeyboard.editor.KeyAction
import com.creategoodthings.markdownKeyboard.editor.MarkdownSyntax
import com.creategoodthings.markdownKeyboard.editor.Snapshot
import com.creategoodthings.markdownKeyboard.editor.TextEdit
import com.creategoodthings.markdownKeyboard.editor.document.Line
import com.creategoodthings.markdownKeyboard.editor.document.LineParser
import com.creategoodthings.markdownKeyboard.editor.document.LineType
import com.creategoodthings.markdownKeyboard.editor.document.ListBlocks
import com.creategoodthings.markdownKeyboard.editor.document.Skeletons
import com.creategoodthings.markdownKeyboard.editor.document.lineAt

/**
 * Backspace deletes markdown structure before it deletes characters.
 *
 * Standing just after a list marker removes a nesting level, then the marker itself, rather
 * than eating the space out of `- ` and leaving a line that no longer parses as anything.
 */
object BackspaceHandler : KeyHandler {
    override fun contextNeed(action: KeyAction): ContextNeed? =
        if (action == KeyAction.Backspace) ContextNeed.EnclosingBlock else null

    override fun handle(snapshot: Snapshot, action: KeyAction): TextEdit? {
        if (snapshot.hasSelection) return TextEdit.insert(snapshot, "")

        val caret = snapshot.selectionStart
        if (caret == 0) return null

        val lines = LineParser.parse(snapshot.text)
        val line = lines.lineAt(caret) ?: return null
        val column = caret - line.start
        val type = line.type

        if (type is LineType.ListItem && column == type.contentColumn) {
            return unwindListItem(snapshot, lines)
        }

        // Sitting in a line's leading whitespace: take out a whole indent level at a time.
        if (column > 0 && line.text.take(column).isBlank()) {
            val unit = MarkdownSyntax.INDENT_UNIT
            val step = if (line.text.take(column).endsWith(unit)) unit.length else 1
            return TextEdit(caret - step, caret, "", caret - step)
        }

        emptyStylePair(snapshot, caret)?.let { return it }
        emptySkeleton(snapshot, lines, line, caret)?.let { return it }

        val step = charactersBefore(snapshot.text, caret)
        return TextEdit(caret - step, caret, "", caret - step)
    }

    private fun unwindListItem(snapshot: Snapshot, lines: List<Line>): TextEdit? {
        val located = ListBlocks.at(lines, snapshot.selectionStart) ?: return null
        val block = located.block
        val index = block.cursor.entry
        val mutated = if (block.currentEntry.level > 0) block.changeIndent(index, -1)
        else block.clearEntry(index)
        return ListEdits.commit(snapshot, located, mutated)
    }

    /** `**|**` and friends disappear whole, so an abandoned style leaves nothing behind. */
    private fun emptyStylePair(snapshot: Snapshot, caret: Int): TextEdit? {
        val byLongestFirst = InlineStyle.entries.sortedByDescending { it.delimiter.length }
        for (style in byLongestFirst) {
            val delimiter = style.delimiter
            val opens = caret >= delimiter.length &&
                snapshot.text.startsWith(delimiter, caret - delimiter.length)
            val closes = snapshot.text.startsWith(delimiter, caret)
            if (opens && closes) {
                val start = caret - delimiter.length
                return TextEdit(start, caret + delimiter.length, "", start)
            }
        }
        return null
    }

    /**
     * An abandoned skeleton goes back whole, the way an empty style pair already does.
     *
     * A table or a fenced block takes the newline that put it on its own line with it, by the same
     * rule `InsertionHandler` used to add one.
     */
    private fun emptySkeleton(snapshot: Snapshot, lines: List<Line>, line: Line, caret: Int): TextEdit? {
        Skeletons.emptyInlineTargetAt(line.text, caret - line.start)?.let { columns ->
            val start = line.start + columns.first
            return TextEdit(start, line.start + columns.last + 1, "", start)
        }

        val run = Skeletons.emptyTableAround(lines, line)
            ?: Skeletons.emptyFencedBlockAround(lines, line)
            ?: return null
        if (!Skeletons.runIsFullyVisible(lines, run, snapshot.reachedStart, snapshot.reachedEnd)) {
            return null
        }
        if (caret <= run.first) return null

        val start = if (run.first > 0) run.first - 1 else run.first
        return TextEdit(start, run.last + 1, "", start)
    }

    /** Surrogate pairs are one character to the user, so they go together. */
    private fun charactersBefore(text: String, caret: Int): Int =
        if (caret >= 2 && text[caret - 1].isLowSurrogate() && text[caret - 2].isHighSurrogate()) 2
        else 1
}
