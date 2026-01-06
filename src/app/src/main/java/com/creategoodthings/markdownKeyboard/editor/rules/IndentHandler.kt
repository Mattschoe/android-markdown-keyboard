package com.creategoodthings.markdownKeyboard.editor.rules

import com.creategoodthings.markdownKeyboard.editor.ContextNeed
import com.creategoodthings.markdownKeyboard.editor.KeyAction
import com.creategoodthings.markdownKeyboard.editor.MarkdownSyntax
import com.creategoodthings.markdownKeyboard.editor.Snapshot
import com.creategoodthings.markdownKeyboard.editor.TextEdit
import com.creategoodthings.markdownKeyboard.editor.document.Line
import com.creategoodthings.markdownKeyboard.editor.document.LineParser
import com.creategoodthings.markdownKeyboard.editor.document.LineType
import com.creategoodthings.markdownKeyboard.editor.document.ListBlocks
import com.creategoodthings.markdownKeyboard.editor.document.lineAt

/**
 * Indent and outdent.
 *
 * On a list item this renests through the block, so the numbers on both the old level and the
 * new one come out right; anywhere else it is plain whitespace on the current line.
 */
object IndentHandler : KeyHandler {
    override fun contextNeed(action: KeyAction): ContextNeed? = when (action) {
        KeyAction.IndentForward, KeyAction.IndentBack -> ContextNeed.EnclosingBlock
        else -> null
    }

    override fun handle(snapshot: Snapshot, action: KeyAction): TextEdit? {
        val delta = if (action == KeyAction.IndentForward) 1 else -1
        val lines = LineParser.parse(snapshot.text)
        val line = lines.lineAt(snapshot.selectionStart) ?: return null

        if (line.type is LineType.ListItem) {
            val located = ListBlocks.at(lines, snapshot.selectionStart) ?: return null
            val block = located.block
            return ListEdits.commit(snapshot, located, block.changeIndent(block.cursor.entry, delta))
        }
        return plainLine(snapshot, line, delta)
    }

    private fun plainLine(snapshot: Snapshot, line: Line, delta: Int): TextEdit? {
        val unit = MarkdownSyntax.INDENT_UNIT
        val newText = if (delta > 0) {
            unit + line.text
        } else {
            // Outdenting a partially indented line takes what is there rather than nothing.
            val removable = line.text.takeWhile { it == ' ' }.length.coerceAtMost(unit.length)
            line.text.drop(removable)
        }
        val shift = newText.length - line.text.length
        val caret = (snapshot.selectionStart + shift).coerceIn(line.start, line.start + newText.length)
        return TextEdit.replacingRegion(snapshot, line.start, line.end, newText, caret)
    }
}
