package com.creategoodthings.markdownKeyboard.ime

import android.view.inputmethod.InputConnection
import com.creategoodthings.markdownKeyboard.editor.ContextNeed
import com.creategoodthings.markdownKeyboard.editor.Snapshot
import com.creategoodthings.markdownKeyboard.editor.document.LineParser
import com.creategoodthings.markdownKeyboard.editor.document.LineType
import com.creategoodthings.markdownKeyboard.editor.document.lineAt

/**
 * Reads the text around the cursor straight out of the editor, fresh, on every contextual key.
 *
 * Keeping a mirror of the field would be cheaper, and would also be wrong: the user taps to
 * move the caret, the host app autocorrects, another keyboard types for a while. A stale mirror
 * does not fail loudly, it computes a confident edit against text that is no longer there. So
 * the read is deliberately unconditional; only the keys that need context pay for it, and plain
 * typing never does.
 */
object SnapshotReader {
    /** Tried in order until the surrounding list is fully inside the window. */
    private val BLOCK_WINDOWS = intArrayOf(512, 2048, 8192)
    private const val LINE_WINDOW = 1024

    fun read(conn: InputConnection, need: ContextNeed): Snapshot? = when (need) {
        ContextNeed.None -> null
        ContextNeed.CurrentLine -> window(conn, LINE_WINDOW, LINE_WINDOW)
        ContextNeed.EnclosingBlock -> enclosingBlock(conn)
        is ContextNeed.Window -> window(conn, need.before, need.after)
    }

    private fun window(conn: InputConnection, before: Int, after: Int): Snapshot? {
        val surrounding = conn.getSurroundingText(before, after, 0) ?: return null
        val text = surrounding.text.toString()
        val start = minOf(surrounding.selectionStart, surrounding.selectionEnd)
            .coerceIn(0, text.length)
        val end = maxOf(surrounding.selectionStart, surrounding.selectionEnd)
            .coerceIn(start, text.length)

        return Snapshot(
            text = text,
            selectionStart = start,
            selectionEnd = end,
            // Getting back less than we asked for means we hit the real edge of the document.
            reachedStart = surrounding.offset == 0 || start < before,
            reachedEnd = text.length - end < after,
            windowStart = surrounding.offset,
        )
    }

    /**
     * Widens the window until the list around the caret fits inside it.
     *
     * Renumbering rewrites lines the user cannot see, so it has to be certain it found the end
     * of the list rather than the end of the window.
     */
    private fun enclosingBlock(conn: InputConnection): Snapshot? {
        var snapshot: Snapshot? = null
        for (size in BLOCK_WINDOWS) {
            snapshot = window(conn, size, size) ?: return snapshot
            if (blockIsComplete(snapshot)) return snapshot
        }
        return snapshot
    }

    private fun blockIsComplete(snapshot: Snapshot): Boolean {
        val lines = LineParser.parse(snapshot.text)
        val cursorLine = lines.lineAt(snapshot.selectionStart) ?: return true

        var first = cursorLine.index
        while (first > 0 && lines[first - 1].type is LineType.ListItem) first--
        var last = cursorLine.index
        while (last < lines.lastIndex && lines[last + 1].type is LineType.ListItem) last++

        val sawTop = snapshot.reachedStart || first > 0
        val sawBottom = snapshot.reachedEnd || last < lines.lastIndex
        return sawTop && sawBottom
    }
}
