package com.creategoodthings.markdownKeyboard

import com.creategoodthings.markdownKeyboard.editor.KeyAction
import com.creategoodthings.markdownKeyboard.editor.MarkdownEngine
import com.creategoodthings.markdownKeyboard.editor.Snapshot
import com.creategoodthings.markdownKeyboard.editor.TextEdit
import com.creategoodthings.markdownKeyboard.ime.EditApplier
import com.creategoodthings.markdownKeyboard.ime.FakeInputConnection
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue

/**
 * Presses [action] on [before] and checks the result reads as [after].
 *
 * Three things are checked at once, so every case in the suite pays for all three:
 *
 *  1. the pure edit rewrites the text as expected;
 *  2. the edited span still contains the caret, which `EditApplier` depends on;
 *  3. replaying the same edit through `EditApplier` against a fake `InputConnection` lands on
 *     exactly the same text and caret. That closes the loop between the pure layer and a real
 *     editor, and is the highest-value assertion in the suite.
 *
 * Pass [Marked.NO_EDIT] as [after] when returning null is the correct outcome.
 */
fun assertKey(action: KeyAction, before: String, after: String) {
    val snapshot = Marked.snapshot(before)
    val edit = MarkdownEngine.edit(snapshot, action)

    if (after == Marked.NO_EDIT) {
        assertNull("expected no edit for $action on \"${before.escaped()}\"", edit)
        return
    }
    assertNotNull("expected an edit for $action on \"${before.escaped()}\"", edit)
    edit!!

    assertEquals(
        "$action on \"${before.escaped()}\"",
        after.escaped(),
        applyPurely(snapshot, edit).escaped(),
    )
    assertCursorContained(snapshot, edit)
    assertAppliesTheSame(snapshot, edit, after)
}

/**
 * As [assertKey], but checks the computation only.
 *
 * Reserved for the cases that knowingly break cursor containment — see
 * `KnownLimitationsTest`, where a multi-line selection is documented as unsupported.
 */
fun assertPureKey(action: KeyAction, before: String, after: String) {
    val snapshot = Marked.snapshot(before)
    val edit = MarkdownEngine.edit(snapshot, action)

    if (after == Marked.NO_EDIT) {
        assertNull(edit)
        return
    }
    assertNotNull(edit)
    assertEquals(
        "$action on \"${before.escaped()}\"",
        after.escaped(),
        applyPurely(snapshot, edit!!).escaped(),
    )
}

/** The edit as arithmetic on the snapshot text, independent of any editor. */
fun applyPurely(snapshot: Snapshot, edit: TextEdit): String {
    val result = snapshot.text.take(edit.replaceStart) +
        edit.replacement +
        snapshot.text.drop(edit.replaceEnd)
    return Marked.render(result, edit.newSelectionStart, edit.newSelectionEnd)
}

/** `TextEdit.replacingRegion` widens its trim back out so this always holds. */
fun assertCursorContained(snapshot: Snapshot, edit: TextEdit) {
    assertTrue(
        "edit starts after the caret: ${edit.replaceStart} > ${snapshot.selectionStart}",
        edit.replaceStart <= snapshot.selectionStart,
    )
    assertTrue(
        "edit ends before the caret: ${edit.replaceEnd} < ${snapshot.selectionEnd}",
        edit.replaceEnd >= snapshot.selectionEnd,
    )
}

private fun assertAppliesTheSame(snapshot: Snapshot, edit: TextEdit, after: String) {
    val conn = FakeInputConnection(snapshot.text, snapshot.selectionStart, snapshot.selectionEnd)
    EditApplier.apply(conn, snapshot, edit)
    assertEquals("EditApplier disagrees with the pure edit", after.escaped(), conn.marked().escaped())
    assertEquals("batch edit left open", 0, conn.batchDepth)
}

fun String.escaped(): String = replace("\n", "\\n").replace("\t", "\\t")
