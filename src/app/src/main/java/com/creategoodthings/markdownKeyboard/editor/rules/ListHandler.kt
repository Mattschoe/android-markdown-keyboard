package com.creategoodthings.markdownKeyboard.editor.rules

import com.creategoodthings.markdownKeyboard.editor.ContextNeed
import com.creategoodthings.markdownKeyboard.editor.KeyAction
import com.creategoodthings.markdownKeyboard.editor.ListKind
import com.creategoodthings.markdownKeyboard.editor.Snapshot
import com.creategoodthings.markdownKeyboard.editor.TextEdit
import com.creategoodthings.markdownKeyboard.editor.document.Line
import com.creategoodthings.markdownKeyboard.editor.document.LineParser
import com.creategoodthings.markdownKeyboard.editor.document.LineType
import com.creategoodthings.markdownKeyboard.editor.document.ListBlocks
import com.creategoodthings.markdownKeyboard.editor.document.lineAt

/**
 * The list keys: turn the current line into a bullet, a number or a checkbox, or turn it back.
 *
 * Pressing the key a line already carries removes the marker, so each key is its own undo.
 */
object ListHandler : KeyHandler {
    override fun contextNeed(action: KeyAction): ContextNeed? = when (action) {
        is KeyAction.ToggleList, KeyAction.NormalizeList -> ContextNeed.EnclosingBlock
        else -> null
    }

    override fun handle(snapshot: Snapshot, action: KeyAction): TextEdit? {
        val lines = LineParser.parse(snapshot.text)
        val line = lines.lineAt(snapshot.selectionStart) ?: return null
        val located = ListBlocks.at(lines, snapshot.selectionStart)

        if (action == KeyAction.NormalizeList) {
            located ?: return null
            return ListEdits.commit(snapshot, located, located.block)
        }

        val kind = (action as? KeyAction.ToggleList)?.kind ?: return null

        if (located != null) {
            val block = located.block
            val index = block.cursor.entry
            val current = block.entries[index].marker
            val replacement = if (current?.kind == kind) null else ListEdits.markerFor(kind)
            return ListEdits.commit(snapshot, located, block.setMarker(index, replacement))
        }
        return markPlainLine(snapshot, line, kind)
    }

    private fun markPlainLine(snapshot: Snapshot, line: Line, kind: ListKind): TextEdit? {
        if (line.type is LineType.CodeFence) return null

        val indent = line.text.takeWhile { it == ' ' || it == '\t' }
        val content = line.text.substring(indent.length)
        val marker = ListEdits.markerFor(kind).render()
        val newText = indent + marker + content
        val caret = (snapshot.selectionStart + marker.length)
            .coerceIn(line.start + indent.length + marker.length, line.start + newText.length)
        return TextEdit.replacingRegion(snapshot, line.start, line.end, newText, caret)
    }
}
