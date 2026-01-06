package com.creategoodthings.markdownKeyboard.editor.rules

import com.creategoodthings.markdownKeyboard.editor.ContextNeed
import com.creategoodthings.markdownKeyboard.editor.KeyAction
import com.creategoodthings.markdownKeyboard.editor.MarkdownSyntax
import com.creategoodthings.markdownKeyboard.editor.Snapshot
import com.creategoodthings.markdownKeyboard.editor.TextEdit

/**
 * Skeletons the user then fills in: links, images, tables, fenced code and rules.
 *
 * Each one leaves the caret where typing continues naturally, and each block-level skeleton
 * starts its own line rather than appending to whatever the caret was already in.
 */
object InsertionHandler : KeyHandler {
    override fun contextNeed(action: KeyAction): ContextNeed? = when (action) {
        KeyAction.InsertLink,
        KeyAction.InsertImage,
        KeyAction.InsertTable,
        KeyAction.InsertCodeBlock,
        KeyAction.InsertHorizontalRule -> ContextNeed.CurrentLine

        else -> null
    }

    override fun handle(snapshot: Snapshot, action: KeyAction): TextEdit? = when (action) {
        KeyAction.InsertLink -> inlineTarget(snapshot, "[")
        KeyAction.InsertImage -> inlineTarget(snapshot, "![")
        KeyAction.InsertCodeBlock ->
            fencedBlock(snapshot, snapshot.selectionStart, snapshot.selectionEnd, snapshot.selectedText)

        KeyAction.InsertTable ->
            ownLine(snapshot, MarkdownSyntax.TABLE_SKELETON, MarkdownSyntax.TABLE_CARET)

        KeyAction.InsertHorizontalRule -> ownLine(snapshot, "---", 3)
        else -> null
    }

    /**
     * A fenced block over `[replaceStart, replaceEnd)` with [body] inside it and the caret at the
     * end of that body.
     *
     * Takes its own range rather than the selection because the code key escalates an abandoned
     * inline pair into a block — see [InlineStyleHandler] — and that pair starts before the caret.
     */
    internal fun fencedBlock(
        snapshot: Snapshot,
        replaceStart: Int,
        replaceEnd: Int,
        body: String,
    ): TextEdit {
        val lead = leadingBreak(snapshot, replaceStart)
        val fence = MarkdownSyntax.FENCE
        return TextEdit(
            replaceStart = replaceStart,
            replaceEnd = replaceEnd,
            replacement = "$lead$fence\n$body\n$fence",
            newSelectionStart = replaceStart + lead.length + fence.length + 1 + body.length,
        )
    }

    /** With text selected the label is already known, so the caret goes to the URL instead. */
    private fun inlineTarget(snapshot: Snapshot, opening: String): TextEdit {
        val label = snapshot.selectedText
        val text = "$opening$label]()"
        val caret = if (label.isEmpty()) opening.length else text.length - 1
        return TextEdit(snapshot.selectionStart, snapshot.selectionEnd, text, snapshot.selectionStart + caret)
    }

    private fun ownLine(snapshot: Snapshot, body: String, caretInBody: Int): TextEdit {
        val lead = leadingBreak(snapshot, snapshot.selectionStart)
        return TextEdit(
            replaceStart = snapshot.selectionStart,
            replaceEnd = snapshot.selectionEnd,
            replacement = lead + body,
            newSelectionStart = snapshot.selectionStart + lead.length + caretInBody,
        )
    }

    /** Nothing before [at] on this line means the skeleton already has a line to itself. */
    private fun leadingBreak(snapshot: Snapshot, at: Int): String {
        val lineStart = snapshot.lineStartAt(at)
        val before = snapshot.text.substring(lineStart, at)
        return if (before.isBlank()) "" else "\n"
    }
}
