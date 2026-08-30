package com.creategoodthings.markdownKeyboard.ime

import com.creategoodthings.markdownKeyboard.Marked
import com.creategoodthings.markdownKeyboard.editor.Snapshot
import com.creategoodthings.markdownKeyboard.editor.TextEdit
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

/**
 * The seam where a correct [TextEdit] can still produce wrong text.
 *
 * The property that matters most — every rule case replayed through a fake editor — is asserted
 * inside `assertKey`, so it runs for all ~120 rule cases rather than only the ones written here.
 * What is left is the mechanics of the applier itself.
 */
class EditApplierTest {

    private fun snapshot(marked: String, windowStart: Int = 0): Snapshot {
        val (text, start, end) = Marked.parse(marked)
        return Snapshot(text, start, end, windowStart = windowStart)
    }

    private fun connectionFor(snapshot: Snapshot, failOnCommit: Boolean = false) =
        FakeInputConnection(
            snapshot.text,
            snapshot.selectionStart,
            snapshot.selectionEnd,
            failOnCommit = failOnCommit,
        )

    @Test fun anEditSpanningTheCaretIsAppliedAroundIt() {
        val snap = snapshot("abc▮def")
        val conn = connectionFor(snap)
        EditApplier.apply(conn, snap, TextEdit(1, 5, "XY", 2))

        assertEquals("aXYf", conn.text)
        assertEquals(2, conn.selStart)
        assertEquals("aX▮Yf", conn.marked())
    }

    @Test fun aSelectionIsDeletedBeforeTheReplacementIsCommitted() {
        val snap = snapshot("a«bcd»e")
        val conn = connectionFor(snap)
        EditApplier.apply(conn, snap, TextEdit(1, 4, "Z", 2))

        assertEquals("aZe", conn.text)
        assertEquals("aZ▮e", conn.marked())
    }

    @Test fun aResultingSelectionIsRestoredInAbsoluteOffsets() {
        val snap = snapshot("x «ab» y", windowStart = 100)
        val conn = connectionFor(snap)
        EditApplier.apply(conn, snap, TextEdit(2, 4, "**ab**", 4, 6))

        assertEquals("x **ab** y", conn.text)
        assertEquals(listOf("setSelection(104, 106)"), conn.log.filter { it.startsWith("setSelection") })
    }

    /** With no window offset there is no absolute frame to set a selection in, so it is skipped. */
    @Test fun withoutAWindowOffsetTheSelectionIsLeftAsACaret() {
        val snap = Snapshot("x ab y", 2, 4, windowStart = -1)
        val conn = FakeInputConnection(snap.text, 2, 4)
        EditApplier.apply(conn, snap, TextEdit(2, 4, "**ab**", 4, 6))

        assertEquals("x **ab** y", conn.text)
        assertFalse(
            "restored a selection with no absolute frame to do it in",
            conn.log.any { it.startsWith("setSelection") },
        )
        assertEquals(4, conn.selStart)
        assertEquals(4, conn.selEnd)
    }

    @Test fun nothingIsDeletedWhenTheEditIsAPureInsertionAtTheCaret() {
        val snap = snapshot("ab▮cd")
        val conn = connectionFor(snap)
        EditApplier.apply(conn, snap, TextEdit(2, 2, "XY", 4))

        assertEquals("abXYcd", conn.text)
        assertFalse(
            "deleteSurroundingText called with nothing to delete",
            conn.log.any { it.startsWith("deleteSurroundingText") },
        )
    }

    @Test fun theReplacementIsSplitSoTheCaretLandsInsideIt() {
        val snap = snapshot("▮")
        val conn = connectionFor(snap)
        EditApplier.apply(conn, snap, TextEdit(0, 0, "[]()", 1))

        assertEquals("[▮]()", conn.marked())
        assertEquals(
            listOf("commitText(\"[\", 1)", "commitText(\"]()\", 0)"),
            conn.log.filter { it.startsWith("commitText") },
        )
    }

    @Test fun aCaretAtTheEndOfTheReplacementNeedsOnlyOneCommit() {
        val snap = snapshot("▮")
        val conn = connectionFor(snap)
        EditApplier.apply(conn, snap, TextEdit(0, 0, "---", 3))

        assertEquals("---▮", conn.marked())
        assertEquals(1, conn.log.count { it.startsWith("commitText") })
    }

    @Test fun theWholeEditIsOneBatch() {
        val snap = snapshot("ab▮")
        val conn = connectionFor(snap)
        EditApplier.apply(conn, snap, TextEdit(2, 2, "c", 3))

        assertEquals("beginBatchEdit", conn.log.first())
        assertEquals("endBatchEdit", conn.log.last())
        assertEquals(1, conn.maxBatchDepth)
        assertEquals(0, conn.batchDepth)
    }

    /** An editor left inside an open batch stops redrawing, so the batch must always close. */
    @Test fun theBatchIsClosedEvenWhenTheEditorThrows() {
        val snap = snapshot("ab▮")
        val conn = connectionFor(snap, failOnCommit = true)

        try {
            EditApplier.apply(conn, snap, TextEdit(2, 2, "c", 3))
            fail("expected the editor's failure to propagate")
        } catch (expected: IllegalStateException) {
            // the point of the test is what happens next
        }
        assertEquals(0, conn.batchDepth)
        assertEquals("endBatchEdit", conn.log.last())
    }

    @Test fun composingTextIsFinishedBeforeAnythingIsWritten() {
        val snap = snapshot("ab▮")
        val conn = connectionFor(snap)
        EditApplier.apply(conn, snap, TextEdit(2, 2, "c", 3))

        val finish = conn.log.indexOf("finishComposingText")
        val firstWrite = conn.log.indexOfFirst { it.startsWith("commitText") }
        assertTrue(finish in 0 until firstWrite)
    }

    @Test fun deletionOnBothSidesOfTheCaretIsOneCall() {
        val snap = snapshot("abc▮def")
        val conn = connectionFor(snap)
        EditApplier.apply(conn, snap, TextEdit(1, 5, "", 1))

        assertEquals("a▮f", conn.marked())
        assertEquals(listOf("deleteSurroundingText(2, 2)"), conn.log.filter { it.startsWith("delete") })
    }
}
