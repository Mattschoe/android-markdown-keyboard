package com.creategoodthings.markdownKeyboard.ime

import android.view.inputmethod.InputConnection
import com.creategoodthings.markdownKeyboard.editor.Snapshot
import com.creategoodthings.markdownKeyboard.editor.TextEdit

/**
 * Applies a [TextEdit] to the editor.
 *
 * Works entirely in cursor-relative terms, because [TextEdit.replacingRegion] guarantees the
 * edited span contains the caret. Absolute document offsets are used only to restore a
 * multi-character selection, and only when the editor was willing to tell us where we were.
 */
object EditApplier {
    fun apply(conn: InputConnection, snapshot: Snapshot, edit: TextEdit) {
        conn.beginBatchEdit()
        try {
            conn.finishComposingText()
            if (snapshot.hasSelection) conn.commitText("", 1)

            val before = (snapshot.selectionStart - edit.replaceStart).coerceAtLeast(0)
            val after = (edit.replaceEnd - snapshot.selectionEnd).coerceAtLeast(0)
            if (before > 0 || after > 0) conn.deleteSurroundingText(before, after)

            // Splitting the commit is what positions the caret: committing the tail with
            // newCursorPosition = 0 leaves the caret at the tail's start.
            val caret = (edit.newSelectionStart - edit.replaceStart)
                .coerceIn(0, edit.replacement.length)
            conn.commitText(edit.replacement.substring(0, caret), 1)
            if (caret < edit.replacement.length) {
                conn.commitText(edit.replacement.substring(caret), 0)
            }

            val keepsSelection = edit.newSelectionEnd > edit.newSelectionStart
            if (keepsSelection && snapshot.windowStart >= 0) {
                conn.setSelection(
                    snapshot.windowStart + edit.newSelectionStart,
                    snapshot.windowStart + edit.newSelectionEnd,
                )
            }
        } finally {
            conn.endBatchEdit()
        }
    }
}
