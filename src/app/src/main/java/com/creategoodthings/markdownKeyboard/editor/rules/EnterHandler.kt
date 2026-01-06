package com.creategoodthings.markdownKeyboard.editor.rules

import com.creategoodthings.markdownKeyboard.editor.ContextNeed
import com.creategoodthings.markdownKeyboard.editor.KeyAction
import com.creategoodthings.markdownKeyboard.editor.Snapshot
import com.creategoodthings.markdownKeyboard.editor.TextEdit
import com.creategoodthings.markdownKeyboard.editor.document.Line
import com.creategoodthings.markdownKeyboard.editor.document.LineParser
import com.creategoodthings.markdownKeyboard.editor.document.LineType
import com.creategoodthings.markdownKeyboard.editor.document.ListBlocks
import com.creategoodthings.markdownKeyboard.editor.document.lineAt

/**
 * Enter continues the structure the caret is standing in.
 *
 * Inside a list this is a structural split of the whole block rather than a guess made from
 * the line above, so pressing Enter halfway down a list renumbers everything below it exactly
 * as pressing Enter at the end appends to it. There is no separate mid-list code path.
 */
object EnterHandler : KeyHandler {
    override fun contextNeed(action: KeyAction): ContextNeed? =
        if (action == KeyAction.Enter) ContextNeed.EnclosingBlock else null

    override fun handle(snapshot: Snapshot, action: KeyAction): TextEdit? {
        // With a selection, Enter replaces it with a plain break; continuation is a caret idea.
        if (snapshot.hasSelection) return TextEdit.insert(snapshot, "\n")

        val lines = LineParser.parse(snapshot.text)
        val line = lines.lineAt(snapshot.selectionStart) ?: return TextEdit.insert(snapshot, "\n")

        return when (val type = line.type) {
            is LineType.ListItem -> inList(snapshot, lines)
            is LineType.Quote -> inQuote(snapshot, line.start, line.end, line.text, type)
            else -> TextEdit.insert(snapshot, "\n")
        }
    }

    private fun inList(snapshot: Snapshot, lines: List<Line>): TextEdit? {
        val located = ListBlocks.at(lines, snapshot.selectionStart)
            ?: return TextEdit.insert(snapshot, "\n")
        val block = located.block
        val index = block.cursor.entry

        val mutated = if (block.currentEntry.content.isBlank()) {
            // An empty item is how you leave a list: step out one level per press, then drop
            // the marker entirely once there is nowhere shallower to go.
            if (block.currentEntry.level > 0) block.changeIndent(index, -1)
            else block.clearEntry(index)
        } else {
            block.splitAtCursor()
        }
        return ListEdits.commit(snapshot, located, mutated)
    }

    private fun inQuote(
        snapshot: Snapshot,
        lineStart: Int,
        lineEnd: Int,
        lineText: String,
        type: LineType.Quote,
    ): TextEdit? {
        val content = lineText.substring(type.contentColumn.coerceAtMost(lineText.length))
        // An empty quote line exits the quote, mirroring how an empty list item exits the list.
        if (content.isBlank()) {
            return TextEdit.replacingRegion(snapshot, lineStart, lineEnd, "", lineStart)
        }
        return TextEdit.insert(snapshot, "\n" + lineText.take(type.contentColumn))
    }
}
