package com.creategoodthings.markdownKeyboard.editor.rules

import com.creategoodthings.markdownKeyboard.editor.ContextNeed
import com.creategoodthings.markdownKeyboard.editor.KeyAction
import com.creategoodthings.markdownKeyboard.editor.MarkdownSyntax
import com.creategoodthings.markdownKeyboard.editor.Snapshot
import com.creategoodthings.markdownKeyboard.editor.TextEdit

/** Line-level block markers: the heading cycle and the quote toggle. */
object BlockHandler : KeyHandler {
    private val HEADING = Regex("""^(#{1,6}) (.*)$""")
    private const val QUOTE_PREFIX = "> "

    override fun contextNeed(action: KeyAction): ContextNeed? = when (action) {
        KeyAction.CycleHeading, KeyAction.ToggleQuote -> ContextNeed.CurrentLine
        else -> null
    }

    override fun handle(snapshot: Snapshot, action: KeyAction): TextEdit? {
        val caret = snapshot.selectionStart
        val lineStart = snapshot.lineStartAt(caret)
        val lineEnd = snapshot.lineEndAt(caret)
        val lineText = snapshot.text.substring(lineStart, lineEnd)

        val newText = when (action) {
            KeyAction.CycleHeading -> cycleHeading(lineText)
            KeyAction.ToggleQuote -> toggleQuote(lineText)
            else -> return null
        }

        val shift = newText.length - lineText.length
        val column = (caret - lineStart + shift).coerceIn(0, newText.length)
        return TextEdit.replacingRegion(snapshot, lineStart, lineEnd, newText, lineStart + column)
    }

    /** none -> H1 -> H2 -> H3 -> none, so one key reaches every level the keyboard offers. */
    private fun cycleHeading(line: String): String {
        val match = HEADING.matchEntire(line) ?: return "# $line"
        val level = match.groupValues[1].length
        val content = match.groupValues[2]
        return if (level >= MarkdownSyntax.MAX_HEADING_LEVEL) content
        else "#".repeat(level + 1) + " " + content
    }

    private fun toggleQuote(line: String): String =
        if (line.startsWith(QUOTE_PREFIX)) line.removePrefix(QUOTE_PREFIX) else QUOTE_PREFIX + line
}
