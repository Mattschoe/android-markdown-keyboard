package com.creategoodthings.markdownKeyboard.ime

import com.creategoodthings.markdownKeyboard.editor.ContextNeed
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** How much text gets read, and how often. */
class SnapshotReaderTest {

    /** Long enough that a partial line still parses as a list item when the window cuts it. */
    private fun list(items: Int) = (1..items).joinToString("\n") { "- ${"x".repeat(40)}" }

    @Test fun nothingToReadMeansTheConnectionIsNotTouched() {
        val conn = FakeInputConnection("hello")
        assertNull(SnapshotReader.read(conn, ContextNeed.None))
        assertTrue(conn.surroundingRequests.isEmpty())
        assertTrue(conn.log.isEmpty())
    }

    @Test fun aCurrentLineNeedIsOneRead() {
        val conn = FakeInputConnection("hello", 2)
        val snapshot = SnapshotReader.read(conn, ContextNeed.CurrentLine)!!

        assertEquals(1, conn.surroundingRequests.size)
        assertEquals("hello", snapshot.text)
        assertEquals(2, snapshot.selectionStart)
    }

    @Test fun anExplicitWindowIsAskedForVerbatim() {
        val conn = FakeInputConnection("abcdefghij", 5)
        SnapshotReader.read(conn, ContextNeed.Window(before = 3, after = 2))

        assertEquals(listOf(3 to 2), conn.surroundingRequests)
    }

    @Test fun aWindowIsClippedToTheDocumentAndOffsetAccordingly() {
        val conn = FakeInputConnection("abcdefghij", 5)
        val snapshot = SnapshotReader.read(conn, ContextNeed.Window(before = 3, after = 2))!!

        assertEquals("cdefg", snapshot.text)
        assertEquals(2, snapshot.windowStart)
        assertEquals(3, snapshot.selectionStart)
    }

    @Test fun aShortListFitsInTheFirstWindow() {
        val conn = FakeInputConnection("- a\n- b\n- c", 6)
        val snapshot = SnapshotReader.read(conn, ContextNeed.EnclosingBlock)!!

        assertEquals(listOf(512 to 512), conn.surroundingRequests)
        assertEquals("- a\n- b\n- c", snapshot.text)
        assertTrue(snapshot.reachedStart)
        assertTrue(snapshot.reachedEnd)
    }

    @Test fun aListLongerThanTheFirstWindowWidensUntilItFits() {
        val conn = FakeInputConnection(list(20) + "\n\nafter", 2)
        SnapshotReader.read(conn, ContextNeed.EnclosingBlock)

        assertEquals(listOf(512 to 512, 2048 to 2048), conn.surroundingRequests)
    }

    @Test fun theWideningStopsAtTheWidestWindow() {
        val conn = FakeInputConnection(list(300), 2)
        SnapshotReader.read(conn, ContextNeed.EnclosingBlock)

        assertEquals(listOf(512 to 512, 2048 to 2048, 8192 to 8192), conn.surroundingRequests)
    }

    /**
     * The gap `TruncationTest` is about: past the widest window the reader gives up and says so,
     * currently listens.
     */
    @Test fun aListTooLongForEveryWindowComesBackFlaggedAsTruncated() {
        val conn = FakeInputConnection(list(300), 2)
        val snapshot = SnapshotReader.read(conn, ContextNeed.EnclosingBlock)!!

        assertFalse(snapshot.reachedEnd)
        assertTrue(snapshot.reachedStart)
        assertEquals(8192 + 2, snapshot.text.length)
    }

    @Test fun anUnreadableFieldReadsAsNull() {
        val conn = FakeInputConnection("- a\n- b", 2)
        conn.surroundingTextAvailable = false
        assertNull(SnapshotReader.read(conn, ContextNeed.EnclosingBlock))
        assertNull(SnapshotReader.read(conn, ContextNeed.CurrentLine))
    }

    /** A backwards selection is a real thing editors report; the snapshot must not carry it on. */
    @Test fun aReversedSelectionIsNormalised() {
        val conn = FakeInputConnection("abcdef", 4, 2)
        conn.reportReversedSelection = true
        val snapshot = SnapshotReader.read(conn, ContextNeed.CurrentLine)!!

        assertTrue(snapshot.selectionStart <= snapshot.selectionEnd)
        assertEquals(2, snapshot.selectionStart)
        assertEquals(4, snapshot.selectionEnd)
    }

    @Test fun anEditorThatWillNotSayWhereTheWindowSitsStillReads() {
        val conn = FakeInputConnection("- a\n- b", 6)
        conn.reportedOffset = -1
        val snapshot = SnapshotReader.read(conn, ContextNeed.EnclosingBlock)!!

        assertEquals(-1, snapshot.windowStart)
        assertEquals("- a\n- b", snapshot.text)
    }
}
