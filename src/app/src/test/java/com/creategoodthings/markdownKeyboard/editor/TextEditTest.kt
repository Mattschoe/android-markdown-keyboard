package com.creategoodthings.markdownKeyboard.editor

import com.creategoodthings.markdownKeyboard.assertCursorContained
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** The diff step that turns a rewritten region into a minimal edit. */
class TextEditTest {

    private fun snapshot(text: String, start: Int, end: Int = start) =
        Snapshot(text, start, end, windowStart = 0)

    private fun applied(snapshot: Snapshot, edit: TextEdit) =
        snapshot.text.take(edit.replaceStart) + edit.replacement + snapshot.text.drop(edit.replaceEnd)

    /**
     * Replacing the whole block would work, but it flickers and flattens the host app's undo
     * history, so a renumber that moves one digit must cost one character.
     */
    @Test fun onlyTheCharactersThatMovedAreRewritten() {
        val text = "1. a\n2. b\n3. c"
        val snap = snapshot(text, 10)
        val edit = TextEdit.replacingRegion(snap, 0, text.length, "1. a\n2. b\n4. c", 10)!!

        assertEquals(1, edit.replaceEnd - edit.replaceStart)
        assertEquals("4", edit.replacement)
        assertEquals("1. a\n2. b\n4. c", applied(snap, edit))
    }

    @Test fun aRenumberOfALaterLineDoesNotTouchTheLinesAbove() {
        val text = "1. a\n2. b\n3. c\n4. d"
        val snap = snapshot(text, 17)
        val edit = TextEdit.replacingRegion(snap, 0, text.length, "1. a\n2. b\n3. c\n9. d", 17)!!

        assertTrue("replaced ${edit.replaceEnd - edit.replaceStart} characters", edit.replaceEnd - edit.replaceStart <= 2)
        assertTrue(edit.replaceStart >= 15)
    }

    @Test fun anIdenticalRegionWithAnUnmovedCursorIsNoEditAtAll() {
        val snap = snapshot("hello", 2)
        assertNull(TextEdit.replacingRegion(snap, 0, 5, "hello", 2))
    }

    /** Moving only the caret still has to reach the editor, or the caret never moves. */
    @Test fun anIdenticalRegionWithAMovedCursorIsStillAnEdit() {
        val snap = snapshot("hello", 2)
        val edit = TextEdit.replacingRegion(snap, 0, 5, "hello", 4)
        assertNotNull(edit)
        assertEquals("hello", applied(snap, edit!!))
        assertEquals(4, edit.newSelectionStart)
    }

    @Test fun pureInsertionWorks() {
        val snap = snapshot("ac", 1)
        val edit = TextEdit.replacingRegion(snap, 1, 1, "b", 2)!!
        assertEquals("abc", applied(snap, edit))
    }

    @Test fun pureDeletionWorks() {
        val snap = snapshot("abc", 2)
        val edit = TextEdit.replacingRegion(snap, 1, 2, "", 1)!!
        assertEquals("ac", applied(snap, edit))
    }

    @Test fun aRegionWithNothingInCommonIsReplacedWholesale() {
        val snap = snapshot("xyz", 1)
        val edit = TextEdit.replacingRegion(snap, 0, 3, "abc", 1)!!
        assertEquals("abc", applied(snap, edit))
    }

    /**
     * Load-bearing: `EditApplier` works in cursor-relative terms and would silently lose text if
     * the trim ever moved the edited span off the caret.
     */
    @Test fun theTrimIsWidenedBackOutToCoverTheCaret() {
        val text = "1. a\n2. b\n3. c"
        for (caret in text.indices) {
            val snap = snapshot(text, caret)
            val edit = TextEdit.replacingRegion(snap, 0, text.length, "1. a\n2. b\n4. c", caret)
                ?: continue
            assertCursorContained(snap, edit)
        }
    }

    @Test fun aSelectionInsideTheRegionIsCoveredToo() {
        val text = "1. a\n2. b\n3. c"
        val snap = snapshot(text, 5, 9)
        val edit = TextEdit.replacingRegion(snap, 0, text.length, "1. a\n2. b\n4. c", 5, 9)!!
        assertCursorContained(snap, edit)
    }

    @Test fun insertReplacesTheSelection() {
        val snap = snapshot("abcde", 1, 4)
        val edit = TextEdit.insert(snap, "X")
        assertEquals("aXe", applied(snap, edit))
        assertEquals(2, edit.newSelectionStart)
    }

    @Test fun insertPlacesTheCaretWhereItIsAsked() {
        val snap = snapshot("ab", 1)
        val edit = TextEdit.insert(snap, "()", cursorOffset = 1)
        assertEquals("a()b", applied(snap, edit))
        assertEquals(2, edit.newSelectionStart)
    }
}
